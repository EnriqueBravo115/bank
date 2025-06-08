package dev.enrique.bank.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.web.servlet.MockMvc;

import dev.enrique.bank.config.TestSecurityConfig;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("testing")
@Import(TestSecurityConfig.class)
@Sql(value = {
        "/sql-test/clear-transaction-db.sql",
        "/sql-test/populate-transaction-db.sql" }, executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(value = { "/sql-test/clear-transaction-db.sql" }, executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
public class TransactionControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("[200] GET /api/v1/transactions/account/1 - Get all transactions")
    public void getAllTransactions_ShouldReturnTransactions() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/account/1")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(3)))
                .andExpect(jsonPath("$.items[0].id").value(3))
                .andExpect(jsonPath("$.items[0].amount").value(200.00))
                .andExpect(jsonPath("$.items[2].id").value(1))
                .andExpect(jsonPath("$.items[2].amount").value(100.00))
                .andExpect(jsonPath("$.headers['page-total-count']").value("1"));
    }

    @Test
    @DisplayName("[200] GET /api/v1/transactions/account/1/history - Get transaction history")
    public void getTransactionHistory_ShouldReturnTransactions() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/account/1/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].id").value(3))
                .andExpect(jsonPath("$[0].amount").value(200.00));
    }

    @Test
    @DisplayName("[200] GET /api/v1/transactions/account/1/year/2023 - Get transactions by year")
    public void getTransactionsByYear_ShouldReturnTransactions() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/account/1/year/2023"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(3));
    }

    @Test
    @DisplayName("[200] GET /api/v1/transactions/account/1/reversals - Get transaction reversals")
    public void getTransactionReversals_ShouldReturnReversals() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/account/1/reversals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0))); // No reversals in test data
    }

    @Test
    @DisplayName("[200] POST /api/v1/transactions/accounts - Get transactions for multiple accounts")
    public void getTransactionsForAccounts_ShouldReturnTransactions() throws Exception {
        mockMvc.perform(post("/api/v1/transactions/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[1,2]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    @DisplayName("[200] GET /api/v1/transactions/account/1/group-by-type - Group transactions by type")
    public void groupTransactionsByType_ShouldReturnGroupedTransactions() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/account/1/group-by-type"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.TRANSFER", hasSize(3)));
    }

    @Test
    @DisplayName("[200] GET /api/v1/transactions/account/1/sum-by-type - Sum transactions by type")
    public void sumTransactionsByType_ShouldReturnSums() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/account/1/sum-by-type"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.TRANSFER").value(350.00));
    }

    @Test
    @DisplayName("[200] GET /api/v1/transactions/account/1/year-stats - Get transaction year statistics")
    public void getTransactionYearStatistics_ShouldReturnStats() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/account/1/year-stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(3));
    }

    @Test
    @DisplayName("[200] GET /api/v1/transactions/account/1/partition-by-amount?amount=150 - Partition transactions by amount")
    public void partitionTransactionsByAmount_ShouldReturnPartitioned() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/account/1/partition-by-amount")
                .param("amount", "150"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.true", hasSize(1)))
                .andExpect(jsonPath("$.false", hasSize(2)));
    }

    @Test
    @DisplayName("[200] GET /api/v1/transactions/account/1/type-summary - Get transaction type summary")
    public void getTransactionTypeSummary_ShouldReturnSummary() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/account/1/type-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.TRANSFER").value(containsString("Count: 3")));
    }

    @Test
    @DisplayName("[200] GET /api/v1/transactions/account/1/total-amount - Get total transaction amount")
    public void getTotalTransactionAmount_ShouldReturnTotal() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/account/1/total-amount"))
                .andExpect(status().isOk())
                .andExpect(content().string("350.00"));
    }

    @Test
    @DisplayName("[200] GET /api/v1/transactions/account/1/total-by-type?type=TRANSFER - Get total amount by type")
    public void getTotalAmountByType_ShouldReturnTotal() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/account/1/total-by-type")
                .param("type", "TRANSFER"))
                .andExpect(status().isOk())
                .andExpect(content().string("350.00"));
    }

    @Test
    @DisplayName("[200] GET /api/v1/transactions/transfer-fee?amount=100&currency=USD - Calculate transfer fee")
    public void calculateTransferFee_ShouldReturnFee() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/transfer-fee")
                .param("amount", "100")
                .param("currency", "USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNumber());
    }

    @Test
    @DisplayName("[200] GET /api/v1/transactions/account/1/has-sufficient-funds?amount=100 - Check sufficient funds")
    public void hasSufficientFunds_ShouldReturnBoolean() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/account/1/has-sufficient-funds")
                .param("amount", "100"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    @DisplayName("[200] GET /api/v1/transactions/account/1/transfer-limit - Get transfer limit")
    public void getTransferLimit_ShouldReturnLimit() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/account/1/transfer-limit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNumber());
    }

    @Test
    @DisplayName("[200] GET /api/v1/transactions/account/1/unique-descriptions - Get unique descriptions")
    public void getUniqueTransactionDescriptions_ShouldReturnDescriptions() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/account/1/unique-descriptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    @DisplayName("[200] GET /api/v1/transactions/account/1/all-descriptions - Get all descriptions")
    public void getAllTransactionDescriptions_ShouldReturnDescriptions() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/account/1/all-descriptions"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Test transaction")));
    }

    @Test
    @DisplayName("[200] POST /api/v1/transactions/average-balance - Get formatted average balance")
    public void getFormattedAverageBalance_ShouldReturnAverage() throws Exception {
        mockMvc.perform(post("/api/v1/transactions/average-balance")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[1,2]"))
                .andExpect(status().isOk())
                .andExpect(content().string(startsWith("$")));
    }

    @Test
    @DisplayName("[200] GET /api/v1/transactions/account/1/max-transaction - Find max transaction")
    public void findMaxTransaction_ShouldReturnMax() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/account/1/max-transaction"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3));
    }

    @Test
    @DisplayName("[200] GET /api/v1/transactions/account/1/average-days-between - Get average days between transactions")
    public void getAverageDaysBetweenTransactions_ShouldReturnAverage() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/account/1/average-days-between"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNumber());
    }
}
