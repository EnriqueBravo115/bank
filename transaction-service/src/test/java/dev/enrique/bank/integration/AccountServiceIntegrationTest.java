package dev.enrique.bank.integration;

import static dev.enrique.bank.commons.constants.PathConstants.TRANSACTION;
import static dev.enrique.bank.commons.constants.PathConstants.TRANSACTION_CREATE;
import static dev.enrique.bank.util.JwtTestUtils.OWNER;
import static dev.enrique.bank.util.JwtTestUtils.admin;
import static dev.enrique.bank.util.JwtTestUtils.customer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.enrique.bank.client.AccountClient;
import dev.enrique.bank.commons.dto.request.TransferRequest;
import dev.enrique.bank.commons.enums.Currency;
import dev.enrique.bank.commons.enums.IdentifierType;
import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.config.ServiceTokenProvider;
import dev.enrique.bank.dao.TransactionRepository;
import dev.enrique.bank.model.Transaction;
import dev.enrique.bank.settlement.TransactionSettlement;
import dev.enrique.bank.util.FakeAccountService;
import dev.enrique.bank.util.PostgresContainerConfig;

/**
 * The real Feign client, circuit breaker and fallbacks against a fake account-service over HTTP:
 * checks the headers and JSON that are sent and what is stored for every kind of answer.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AccountServiceIntegrationTest extends PostgresContainerConfig {
    private static final String SOURCE_CLABE = "646180000000000001";
    private static final String TARGET_CLABE = "646180000000000002";
    private static final String LOOKUP = "/api/v1/account/get-by-clabe/";
    private static final String TRANSFER = "/api/v1/account/movement/transfer";
    private static final String MOVEMENT = "/api/v1/account/movement/";
    private static final FakeAccountService accountService = new FakeAccountService();

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private TransactionSettlement settlement;

    // Keycloak is not running: the client credentials token is replaced.
    @MockitoBean
    private ServiceTokenProvider serviceTokenProvider;

    @DynamicPropertySource
    static void accountServiceInstance(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.discovery.client.simple.instances.account-service[0].uri", accountService::url);
    }

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        accountService.reset();
        when(serviceTokenProvider.token()).thenReturn("test-token");
        accountService.respond(LOOKUP + SOURCE_CLABE, 200, account(OWNER, "ACC-1", SOURCE_CLABE));
        accountService.respond(LOOKUP + TARGET_CLABE, 200, account("user-2", "ACC-2", TARGET_CLABE));
    }

    @Test
    void shouldSendTransferWithServiceTokenAndIdempotencyKey() throws Exception {
        accountService.respond(TRANSFER, 200, "{\"transactionStatus\":\"COMPLETED\",\"reason\":\"Valid Transaction\"}");

        transfer().andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionStatus").value("COMPLETED"));

        Transaction saved = singleSavedTransaction();
        assertThat(accountService.received(LOOKUP)).hasSize(2)
                .allSatisfy(request -> assertThat(request.authorization()).isEqualTo("Bearer test-token"));
        List<FakeAccountService.ReceivedRequest> movements = accountService.received(TRANSFER);
        assertThat(movements).hasSize(1);
        assertThat(movements.get(0).authorization()).isEqualTo("Bearer test-token");
        assertThat(movements.get(0).idempotencyKey()).isEqualTo(saved.getTransactionCode());
        JsonNode sent = objectMapper.readTree(movements.get(0).body());
        assertThat(sent.get("sourceAccountNumber").asText()).isEqualTo("ACC-1");
        assertThat(sent.get("targetAccountNumber").asText()).isEqualTo("ACC-2");
        assertThat(sent.get("amount").decimalValue()).isEqualByComparingTo("250.00");
        assertThat(saved.getTransactionStatus()).isEqualTo(TransactionStatus.COMPLETED);
    }

    @Test
    void shouldRecordDeclinedTransferWithMessageOfAccountService() throws Exception {
        accountService.respond(TRANSFER, 422, "{\"status\":422,\"message\":\"Insufficient funds\"}");

        transfer().andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionStatus").value("DECLINED"))
                .andExpect(jsonPath("$.reason").value("Insufficient funds"));

        assertThat(singleSavedTransaction().getTransactionStatus()).isEqualTo(TransactionStatus.DECLINED);
    }

    @Test
    void shouldRecordFailedTransferWhenAccountDisappeared() throws Exception {
        accountService.respond(TRANSFER, 404, "{\"status\":404,\"message\":\"Account not found\"}");

        transfer().andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionStatus").value("FAILED"))
                .andExpect(jsonPath("$.reason").value("Account not found"));
    }

    @Test
    void shouldKeepTransferPendingOnServerErrorAndSettleItWithTheSameKey() throws Exception {
        accountService.respond(TRANSFER, 500, "{\"status\":500,\"message\":\"boom\"}");

        transfer().andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionStatus").value("PENDING"))
                .andExpect(jsonPath("$.reason").value(AccountClient.AWAITING_CONFIRMATION));

        accountService.respond(TRANSFER, 200, "{\"transactionStatus\":\"COMPLETED\",\"reason\":\"Valid Transaction\"}");
        assertThat(settlement.retryPending(LocalDateTime.now().plusMinutes(1))).isOne();

        Transaction saved = singleSavedTransaction();
        assertThat(saved.getTransactionStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(accountService.received(TRANSFER)).hasSize(2)
                .allSatisfy(request -> assertThat(request.idempotencyKey()).isEqualTo(saved.getTransactionCode()));
    }

    @Test
    void shouldReturn404WhenAccountServiceDoesNotKnowTheAccount() throws Exception {
        accountService.respond(LOOKUP + TARGET_CLABE, 404, "{\"status\":404,\"message\":\"Account not found\"}");

        transfer().andExpect(status().isNotFound());

        assertThat(accountService.received(TRANSFER)).isEmpty();
        assertThat(transactionRepository.count()).isZero();
    }

    @Test
    void shouldReverseThroughAccountService() throws Exception {
        accountService.respond(TRANSFER, 200, "{\"transactionStatus\":\"COMPLETED\",\"reason\":\"Valid Transaction\"}");
        transfer().andExpect(status().isOk());
        String code = singleSavedTransaction().getTransactionCode();
        accountService.respond(MOVEMENT + code + "/reverse", 200,
                "{\"transactionStatus\":\"REVERSED\",\"reason\":\"Reversed\"}");

        mockMvc.perform(post(TRANSACTION + "/{code}/reverse", code).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionStatus").value("REVERSED"));

        assertThat(accountService.received(MOVEMENT + code + "/reverse")).hasSize(1);
        assertThat(singleSavedTransaction().getTransactionStatus()).isEqualTo(TransactionStatus.REVERSED);
    }

    private ResultActions transfer() throws Exception {
        TransferRequest request = new TransferRequest(new BigDecimal("250.00"), "Rent payment", Currency.MX,
                SOURCE_CLABE, IdentifierType.CLABE, TARGET_CLABE, IdentifierType.CLABE);
        return mockMvc.perform(post(TRANSACTION_CREATE + "/transfer")
                .with(customer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private Transaction singleSavedTransaction() {
        List<Transaction> all = transactionRepository.findAll();
        assertThat(all).hasSize(1);
        return all.get(0);
    }

    private static String account(String userId, String accountNumber, String clabe) {
        return "{\"userId\":\"" + userId + "\",\"accountNumber\":\"" + accountNumber + "\",\"clabe\":\"" + clabe
                + "\",\"accountStatus\":\"OPEN\"}";
    }
}
