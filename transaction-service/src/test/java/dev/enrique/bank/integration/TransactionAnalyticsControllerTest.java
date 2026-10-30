package dev.enrique.bank.integration;

import static dev.enrique.bank.commons.constants.PathConstants.TRANSACTION_ANALYTICS;
import static dev.enrique.bank.util.JwtTestUtils.admin;
import static dev.enrique.bank.util.JwtTestUtils.customer;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import dev.enrique.bank.util.PostgresContainerConfig;

/**
 * Uses the seed data from db/migration/V2__populate_transactions.sql:
 * CLABE 646180110400000001 has one COMPLETED TRANSFER (1500.00, January 2024).
 */
@SpringBootTest
@AutoConfigureMockMvc
class TransactionAnalyticsControllerTest extends PostgresContainerConfig {
    private static final String CLABE = "646180110400000001";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void groupByType_asAdmin_groupsTransactions() throws Exception {
        mockMvc.perform(get(TRANSACTION_ANALYTICS + "/group-by-type")
                .param("sourceIdentifier", CLABE)
                .param("status", "COMPLETED")
                .with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.TRANSFER", hasSize(1)))
                .andExpect(jsonPath("$.TRANSFER[0].amount").value(1500.00));
    }

    @Test
    void groupByType_asCustomer_returns403() throws Exception {
        mockMvc.perform(get(TRANSACTION_ANALYTICS + "/group-by-type")
                .param("sourceIdentifier", CLABE)
                .param("status", "COMPLETED")
                .with(customer()))
                .andExpect(status().isForbidden());
    }

    @Test
    void sumByType_returnsSumPerType() throws Exception {
        mockMvc.perform(get(TRANSACTION_ANALYTICS + "/sum-by-type")
                .param("sourceIdentifier", CLABE)
                .param("status", "COMPLETED")
                .with(customer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.TRANSFER").value(1500.00));
    }

    @Test
    void totalAmount_returnsTotalForType() throws Exception {
        mockMvc.perform(get(TRANSACTION_ANALYTICS + "/total-amount")
                .param("sourceIdentifier", CLABE)
                .param("status", "COMPLETED")
                .param("type", "TRANSFER")
                .with(customer()))
                .andExpect(status().isOk())
                .andExpect(content().string("1500.00"));
    }

    @Test
    void countByMonth_returnsCountPerMonth() throws Exception {
        mockMvc.perform(get(TRANSACTION_ANALYTICS + "/count-by-month")
                .param("sourceIdentifier", CLABE)
                .param("status", "COMPLETED")
                .with(customer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.JANUARY").value(1));
    }

    @Test
    void averageAmount_returnsAveragePerType() throws Exception {
        mockMvc.perform(get(TRANSACTION_ANALYTICS + "/average-amount")
                .param("sourceIdentifier", CLABE)
                .param("status", "COMPLETED")
                .with(customer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.TRANSFER").value(1500.00));
    }
}
