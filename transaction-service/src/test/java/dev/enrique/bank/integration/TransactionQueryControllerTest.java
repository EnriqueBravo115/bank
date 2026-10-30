package dev.enrique.bank.integration;

import static dev.enrique.bank.commons.constants.PathConstants.TRANSACTION_QUERY;
import static dev.enrique.bank.util.JwtTestUtils.admin;
import static dev.enrique.bank.util.JwtTestUtils.customer;
import static dev.enrique.bank.util.JwtTestUtils.tokenWithRole;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import dev.enrique.bank.util.PostgresContainerConfig;

/**
 * Uses the seed data from db/migration/V2__populate_transactions.sql:
 * CLABE 646180110400000001 has one COMPLETED TRANSFER (TXN-2024-001, 1500.00, 2024-01-15).
 */
@SpringBootTest
@AutoConfigureMockMvc
class TransactionQueryControllerTest extends PostgresContainerConfig {
    private static final String CLABE = "646180110400000001";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void history_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(TRANSACTION_QUERY + "/history")
                .param("sourceIdentifier", CLABE)
                .param("status", "COMPLETED"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void history_withIncompleteRegistration_returns403() throws Exception {
        mockMvc.perform(get(TRANSACTION_QUERY + "/history")
                .param("sourceIdentifier", CLABE)
                .param("status", "COMPLETED")
                .with(tokenWithRole("CUSTOMER_BASIC", "PROFILE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Registration incomplete"));
    }

    @Test
    void history_returnsTransactions() throws Exception {
        mockMvc.perform(get(TRANSACTION_QUERY + "/history")
                .param("sourceIdentifier", CLABE)
                .param("status", "COMPLETED")
                .with(customer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].transactionCode").value("TXN-2024-001"))
                .andExpect(jsonPath("$[0].amount").value(1500.00))
                .andExpect(jsonPath("$[0].transactionType").value("TRANSFER"));
    }

    @Test
    void all_returnsPagedTransactions() throws Exception {
        mockMvc.perform(get(TRANSACTION_QUERY + "/all")
                .param("sourceIdentifier", CLABE)
                .param("status", "COMPLETED")
                .param("page", "0")
                .param("size", "10")
                .with(customer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].amount").value(1500.00))
                .andExpect(jsonPath("$.headers['page-total-count'][0]").value("1"));
    }

    @Test
    void statusYear_filtersByYear() throws Exception {
        mockMvc.perform(get(TRANSACTION_QUERY + "/status-year")
                .param("sourceIdentifier", CLABE)
                .param("status", "COMPLETED")
                .param("year", "2024")
                .with(customer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(get(TRANSACTION_QUERY + "/status-year")
                .param("sourceIdentifier", CLABE)
                .param("status", "COMPLETED")
                .param("year", "2023")
                .with(customer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void bySources_asAdmin_returnsTransactions() throws Exception {
        mockMvc.perform(get(TRANSACTION_QUERY + "/by-sources")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[\"" + CLABE + "\", \"4111111111111111\"]")
                .with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void bySources_asCustomer_returns403() throws Exception {
        mockMvc.perform(get(TRANSACTION_QUERY + "/by-sources")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[\"" + CLABE + "\"]")
                .with(customer()))
                .andExpect(status().isForbidden());
    }

    @Test
    void max_returnsHighestAmount() throws Exception {
        mockMvc.perform(get(TRANSACTION_QUERY + "/max")
                .param("sourceIdentifier", CLABE)
                .param("status", "COMPLETED")
                .with(customer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(1500.00));
    }

    @Test
    void dateRange_filtersByDate() throws Exception {
        mockMvc.perform(get(TRANSACTION_QUERY + "/date-range")
                .param("sourceIdentifier", CLABE)
                .param("status", "COMPLETED")
                .param("startDate", "2024-01-01T00:00:00")
                .param("endDate", "2024-01-31T23:59:59")
                .with(customer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(get(TRANSACTION_QUERY + "/date-range")
                .param("sourceIdentifier", CLABE)
                .param("status", "COMPLETED")
                .param("startDate", "2024-02-01T00:00:00")
                .param("endDate", "2024-02-29T23:59:59")
                .with(customer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void amountRange_filtersByAmount() throws Exception {
        mockMvc.perform(get(TRANSACTION_QUERY + "/amount-range")
                .param("sourceIdentifier", CLABE)
                .param("status", "COMPLETED")
                .param("minAmount", "500.00")
                .param("maxAmount", "2000.00")
                .with(customer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(get(TRANSACTION_QUERY + "/amount-range")
                .param("sourceIdentifier", CLABE)
                .param("status", "COMPLETED")
                .param("minAmount", "1.00")
                .param("maxAmount", "100.00")
                .with(customer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void byTransactionCode_returnsTransaction() throws Exception {
        mockMvc.perform(get(TRANSACTION_QUERY + "/{transactionCode}", "TXN-2024-001")
                .with(customer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionCode").value("TXN-2024-001"))
                .andExpect(jsonPath("$.description").value("Pago de renta enero"))
                .andExpect(jsonPath("$.amount").value(1500.00));
    }

    @Test
    void identifierType_filtersByType() throws Exception {
        mockMvc.perform(get(TRANSACTION_QUERY + "/identifier-type")
                .param("sourceIdentifier", CLABE)
                .param("status", "COMPLETED")
                .param("identifierType", "CLABE")
                .with(customer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void keyword_searchesDescription() throws Exception {
        mockMvc.perform(get(TRANSACTION_QUERY + "/keyword")
                .param("sourceIdentifier", CLABE)
                .param("status", "COMPLETED")
                .param("keyword", "renta")
                .with(customer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].description").value("Pago de renta enero"));
    }
}
