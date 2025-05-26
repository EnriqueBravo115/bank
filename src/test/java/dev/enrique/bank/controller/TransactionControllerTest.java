package dev.enrique.bank.controller;

import static dev.enrique.bank.commons.constants.PathConstants.API_V1_TRANSACTION;
import static dev.enrique.bank.commons.constants.PathConstants.HISTORY;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(value = {
        "/sql-test/clear-transaction-db.sql",
        "/sql-test/populate-transaction-db.sql" }, executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(value = { "/sql-test/clear-transaction-db.sql" }, executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
public class TransactionControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("[200] GET /api/v1/transaction/history/1 - Obtener historial de transacciones")
    public void getTransactionHistory_ShouldReturnTransactions() throws Exception {
        mockMvc.perform(get(API_V1_TRANSACTION + HISTORY + 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].id").value(3))
                .andExpect(jsonPath("$[0].amount").value(200.00))
                .andExpect(jsonPath("$[0].description").value("Test transaction 3"))
                .andExpect(jsonPath("$[0].transactionType").value("TRANSFER"));
    }
}
