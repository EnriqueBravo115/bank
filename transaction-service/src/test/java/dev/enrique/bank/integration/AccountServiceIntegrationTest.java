package dev.enrique.bank.integration;

import static dev.enrique.bank.commons.constants.PathConstants.TRANSACTION_CREATE;
import static dev.enrique.bank.util.JwtTestUtils.customer;
import static dev.enrique.bank.util.TransactionFixtures.CLABE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.enrique.bank.commons.dto.request.TransferRequest;
import dev.enrique.bank.commons.enums.Currency;
import dev.enrique.bank.commons.enums.IdentifierType;
import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.dao.TransactionRepository;
import dev.enrique.bank.model.Transaction;
import dev.enrique.bank.util.FakeAccountService;
import dev.enrique.bank.util.PostgresContainerConfig;

/**
 * The real Feign client against a fake account-service: checks the JSON it sends and that every
 * kind of answer (including errors) ends up as a recorded transaction instead of a 500.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AccountServiceIntegrationTest extends PostgresContainerConfig {
    private static final String TARGET_CLABE = "002180700148685530";
    private static final FakeAccountService accountService = new FakeAccountService();

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private TransactionRepository transactionRepository;

    @DynamicPropertySource
    static void accountServiceInstance(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.discovery.client.simple.instances.account-service[0].uri", accountService::url);
    }

    @BeforeEach
    void cleanUp() {
        transactionRepository.deleteAll();
        accountService.reset();
    }

    @Test
    void shouldSendTransferToAccountServiceAndRecordItAsCompleted() throws Exception {
        accountService.respond(200, "{\"transactionStatus\":\"COMPLETED\",\"reason\":\"Valid Transaction\"}");

        transfer().andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionStatus").value("COMPLETED"));

        assertThat(accountService.received()).hasSize(1);
        assertThat(accountService.received().get(0).path()).isEqualTo("/api/v1/account/movement/transfer");
        JsonNode sent = objectMapper.readTree(accountService.received().get(0).body());
        assertThat(sent.get("sourceAccountNumber").asText()).isEqualTo(CLABE);
        assertThat(sent.get("targetAccountNumber").asText()).isEqualTo(TARGET_CLABE);
        assertThat(sent.get("amount").decimalValue()).isEqualByComparingTo("250.00");
        assertThat(savedStatus()).isEqualTo(TransactionStatus.COMPLETED);
    }

    @Test
    void shouldRecordDeclinedTransferWithMessageOfAccountService() throws Exception {
        accountService.respond(422, "{\"status\":422,\"message\":\"Insufficient funds\"}");

        transfer().andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionStatus").value("DECLINED"))
                .andExpect(jsonPath("$.reason").value("Insufficient funds"));

        assertThat(savedStatus()).isEqualTo(TransactionStatus.DECLINED);
    }

    @Test
    void shouldRecordFailedTransferWhenAccountDoesNotExist() throws Exception {
        accountService.respond(404, "{\"status\":404,\"message\":\"Account not found\"}");

        transfer().andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionStatus").value("FAILED"))
                .andExpect(jsonPath("$.reason").value("Account not found"));

        assertThat(savedStatus()).isEqualTo(TransactionStatus.FAILED);
    }

    @Test
    void shouldRecordFailedTransferWhenAccountServiceErrors() throws Exception {
        accountService.respond(500, "{\"status\":500,\"message\":\"boom\"}");

        transfer().andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionStatus").value("FAILED"))
                .andExpect(jsonPath("$.reason").value("Service unavailable"));

        assertThat(savedStatus()).isEqualTo(TransactionStatus.FAILED);
    }

    private org.springframework.test.web.servlet.ResultActions transfer() throws Exception {
        TransferRequest request = new TransferRequest(new BigDecimal("250.00"), "Pago de renta", Currency.MX,
                CLABE, IdentifierType.CLABE, TARGET_CLABE, IdentifierType.CLABE);
        return mockMvc.perform(post(TRANSACTION_CREATE + "/transfer")
                .with(customer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private TransactionStatus savedStatus() {
        List<Transaction> all = transactionRepository.findAll();
        assertThat(all).hasSize(1);
        return all.get(0).getTransactionStatus();
    }
}
