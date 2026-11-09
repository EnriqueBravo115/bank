package dev.enrique.bank.integration;

import static dev.enrique.bank.util.JwtTestUtils.OTHER_USER;
import static dev.enrique.bank.commons.constants.PathConstants.TRANSACTION_ANALYTICS;
import static dev.enrique.bank.commons.enums.TransactionStatus.COMPLETED;
import static dev.enrique.bank.commons.enums.TransactionStatus.FAILED;
import static dev.enrique.bank.commons.enums.TransactionType.SERVICE;
import static dev.enrique.bank.commons.enums.TransactionType.TRANSFER;
import static dev.enrique.bank.util.JwtTestUtils.admin;
import static dev.enrique.bank.util.JwtTestUtils.customer;
import static dev.enrique.bank.util.TransactionFixtures.ACCOUNT;
import static dev.enrique.bank.util.TransactionFixtures.CLABE;
import static dev.enrique.bank.util.TransactionFixtures.transaction;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import dev.enrique.bank.dao.TransactionRepository;
import dev.enrique.bank.util.PostgresContainerConfig;

/**
 * COMPLETED: two transfers (1500.00 and 500.00, January 2024, 10 days apart) and one service payment
 * (300.00, February 2024, 10 days later). A FAILED transfer must never be counted.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TransactionAnalyticsControllerTest extends PostgresContainerConfig {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        transactionRepository.saveAll(List.of(
                transaction("TXN-001", CLABE, TRANSFER, COMPLETED, "1500.00", "Renta",
                        LocalDateTime.of(2024, 1, 15, 9, 0)),
                transaction("TXN-002", CLABE, TRANSFER, COMPLETED, "500.00", "Ahorro",
                        LocalDateTime.of(2024, 1, 25, 9, 0)),
                transaction("TXN-003", CLABE, SERVICE, COMPLETED, "300.00", "Luz",
                        LocalDateTime.of(2024, 2, 4, 9, 0)),
                transaction("TXN-004", CLABE, TRANSFER, FAILED, "999.00", "Rechazada",
                        LocalDateTime.of(2024, 1, 20, 9, 0))));
    }

    private static MockHttpServletRequestBuilder completed(String path) {
        return get(TRANSACTION_ANALYTICS + path)
                .param("sourceIdentifier", ACCOUNT)
                .param("status", "COMPLETED");
    }

    @Test
    void adminOnlyEndpoints_asCustomer_return403() throws Exception {
        for (String path : List.of("/group-by-type", "/type-summary", "/average-days", "/max-by-type")) {
            mockMvc.perform(completed(path).with(customer()))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(completed("/partition-by-amount").param("amount", "400").with(customer()))
                .andExpect(status().isForbidden());
    }

    @Test
    void groupByType_asAdmin_groupsTransactions() throws Exception {
        mockMvc.perform(completed("/group-by-type").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.TRANSFER", hasSize(2)))
                .andExpect(jsonPath("$.SERVICE", hasSize(1)))
                .andExpect(jsonPath("$.SERVICE[0].amount").value(300.00));
    }

    @Test
    void sumByType_returnsSumPerType() throws Exception {
        mockMvc.perform(completed("/sum-by-type").with(customer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.TRANSFER").value(2000.00))
                .andExpect(jsonPath("$.SERVICE").value(300.00));
    }

    @Test
    void partitionByAmount_splitsAboveAndAtOrBelowThreshold() throws Exception {
        mockMvc.perform(completed("/partition-by-amount").param("amount", "500").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['true']", hasSize(1)))
                .andExpect(jsonPath("$['true'][0].transactionCode").value("TXN-001"))
                .andExpect(jsonPath("$['false']", hasSize(2)));
    }

    @Test
    void typeSummary_returnsCountAndTotalPerType() throws Exception {
        mockMvc.perform(completed("/type-summary").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.TRANSFER.count").value(2))
                .andExpect(jsonPath("$.TRANSFER.total").value(2000.00))
                .andExpect(jsonPath("$.SERVICE.count").value(1))
                .andExpect(jsonPath("$.SERVICE.total").value(300.00));
    }

    @Test
    void totalAmount_returnsTotalForType() throws Exception {
        mockMvc.perform(completed("/total-amount").param("type", "TRANSFER").with(customer()))
                .andExpect(status().isOk())
                .andExpect(content().string("2000.00"));
    }

    @Test
    void averageDays_returnsAverageGapBetweenTransactions() throws Exception {
        mockMvc.perform(completed("/average-days").with(admin()))
                .andExpect(status().isOk())
                .andExpect(content().string("10.0"));
    }

    @Test
    void maxByType_returnsHighestTransactionOfEachType() throws Exception {
        mockMvc.perform(completed("/max-by-type").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.TRANSFER.transactionCode").value("TXN-001"))
                .andExpect(jsonPath("$.TRANSFER.amount").value(1500.00))
                .andExpect(jsonPath("$.SERVICE.amount").value(300.00));
    }

    @Test
    void countByMonth_returnsCountPerMonth() throws Exception {
        mockMvc.perform(completed("/count-by-month").with(customer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.JANUARY").value(2))
                .andExpect(jsonPath("$.FEBRUARY").value(1));
    }

    @Test
    void averageAmount_returnsAveragePerType() throws Exception {
        mockMvc.perform(completed("/average-amount").with(customer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.TRANSFER").value(1000.00))
                .andExpect(jsonPath("$.SERVICE").value(300.00));
    }

    @Test
    void customer_onlyAggregatesOwnTransactions() throws Exception {
        mockMvc.perform(completed("/sum-by-type").with(customer(OTHER_USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.TRANSFER").doesNotExist());
        mockMvc.perform(completed("/sum-by-type").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.TRANSFER").value(2000.00));
    }
}
