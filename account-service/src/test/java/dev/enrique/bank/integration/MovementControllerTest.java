package dev.enrique.bank.integration;

import static dev.enrique.bank.commons.constants.Headers.IDEMPOTENCY_KEY;
import static dev.enrique.bank.commons.constants.PathConstants.MOVEMENT;
import static dev.enrique.bank.util.JwtTestUtils.admin;
import static dev.enrique.bank.util.JwtTestUtils.customer;
import static dev.enrique.bank.util.JwtTestUtils.service;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import dev.enrique.bank.commons.enums.AccountStatus;
import dev.enrique.bank.dao.MovementRepository;
import dev.enrique.bank.util.AccountFixtures;
import dev.enrique.bank.util.PostgresContainerConfig;

@SpringBootTest
@AutoConfigureMockMvc
class MovementControllerTest extends PostgresContainerConfig {
    private static final String SOURCE = "ACC-1";
    private static final String TARGET = "ACC-2";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AccountFixtures fixtures;
    @Autowired
    private MovementRepository movementRepository;

    @BeforeEach
    void setUp() {
        fixtures.clear();
        fixtures.account("user-1", SOURCE, "646180000000000001", AccountStatus.OPEN, "500.00");
        fixtures.account("user-2", TARGET, "646180000000000002", AccountStatus.OPEN, "0.00");
    }

    // ---- security ----

    @Test
    void shouldRejectMovementsFromCustomersAndAdmins() throws Exception {
        transfer("txn-1", "100.00", customer("user-1")).andExpect(status().isForbidden());
        transfer("txn-1", "100.00", admin()).andExpect(status().isForbidden());

        assertThat(fixtures.balanceOf(SOURCE)).isEqualByComparingTo("500.00");
    }

    @Test
    void shouldRejectMovementWithoutToken() throws Exception {
        mockMvc.perform(post(MOVEMENT + "/transfer")
                .header(IDEMPOTENCY_KEY, "txn-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(transferBody("100.00")))
                .andExpect(status().isUnauthorized());
    }

    // ---- validation ----

    @Test
    void shouldRejectInvalidAmountsAndMissingKey() throws Exception {
        transfer("txn-1", "-10.00", service()).andExpect(status().isBadRequest());
        transfer("txn-1", "0", service()).andExpect(status().isBadRequest());

        mockMvc.perform(post(MOVEMENT + "/transfer")
                .with(service())
                .contentType(MediaType.APPLICATION_JSON)
                .content(transferBody("100.00")))
                .andExpect(status().isBadRequest());

        assertThat(fixtures.balanceOf(SOURCE)).isEqualByComparingTo("500.00");
        assertThat(movementRepository.count()).isZero();
    }

    // ---- idempotency ----

    @Test
    void shouldTransferOnceForRepeatedIdempotencyKey() throws Exception {
        transfer("txn-1", "100.00", service())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionStatus").value("COMPLETED"));
        transfer("txn-1", "100.00", service())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionStatus").value("COMPLETED"));

        assertThat(fixtures.balanceOf(SOURCE)).isEqualByComparingTo("400.00");
        assertThat(fixtures.balanceOf(TARGET)).isEqualByComparingTo("100.00");
        assertThat(movementRepository.count()).isOne();
    }

    @Test
    void shouldEvaluateRejectedMovementAgainOnRetry() throws Exception {
        transfer("txn-1", "900.00", service())
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(
                        "Insufficient funds in account " + SOURCE + " to process transaction of 900.00"));
        assertThat(movementRepository.count()).isZero();

        transfer("txn-1", "100.00", service()).andExpect(status().isOk());
        assertThat(fixtures.balanceOf(SOURCE)).isEqualByComparingTo("400.00");
    }

    @Test
    void shouldDebitOnceForEachMovementType() throws Exception {
        debit("/purchase", "txn-p", "{\"accountNumber\":\"" + SOURCE + "\",\"amount\":10.00}");
        debit("/purchase", "txn-p", "{\"accountNumber\":\"" + SOURCE + "\",\"amount\":10.00}");
        debit("/service", "txn-s", "{\"accountNumber\":\"" + SOURCE + "\",\"amount\":20.00}");
        debit("/withdrawal", "txn-w",
                "{\"accountNumber\":\"" + SOURCE + "\",\"amount\":30.00,\"securityVerificationMethod\":\"PIN\"}");

        assertThat(fixtures.balanceOf(SOURCE)).isEqualByComparingTo("440.00");
        assertThat(movementRepository.count()).isEqualTo(3);
    }

    @Test
    void shouldDeclineMovementOfFrozenAccount() throws Exception {
        fixtures.account("user-3", "ACC-3", "646180000000000003", AccountStatus.FROZEN, "500.00");

        mockMvc.perform(post(MOVEMENT + "/service")
                .with(service())
                .header(IDEMPOTENCY_KEY, "txn-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountNumber\":\"ACC-3\",\"amount\":10.00}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Account ACC-3 is FROZEN and cannot operate"));
    }

    @Test
    void shouldReturn404ForUnknownAccount() throws Exception {
        mockMvc.perform(post(MOVEMENT + "/service")
                .with(service())
                .header(IDEMPOTENCY_KEY, "txn-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountNumber\":\"ACC-404\",\"amount\":10.00}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Account not found with number: ACC-404"));
    }

    // ---- reversal and cancellation ----

    @Test
    void shouldReverseCompletedTransferOnce() throws Exception {
        transfer("txn-1", "100.00", service()).andExpect(status().isOk());

        reverse("txn-1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionStatus").value("REVERSED"));
        reverse("txn-1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionStatus").value("REVERSED"));

        assertThat(fixtures.balanceOf(SOURCE)).isEqualByComparingTo("500.00");
        assertThat(fixtures.balanceOf(TARGET)).isEqualByComparingTo("0.00");

        // A late copy of the original request does not move the money again.
        transfer("txn-1", "100.00", service())
                .andExpect(jsonPath("$.transactionStatus").value("REVERSED"));
        assertThat(fixtures.balanceOf(SOURCE)).isEqualByComparingTo("500.00");
    }

    @Test
    void shouldReverseDebitByCreditingTheAccount() throws Exception {
        debit("/service", "txn-s", "{\"accountNumber\":\"" + SOURCE + "\",\"amount\":20.00}");

        reverse("txn-s").andExpect(jsonPath("$.transactionStatus").value("REVERSED"));

        assertThat(fixtures.balanceOf(SOURCE)).isEqualByComparingTo("500.00");
    }

    @Test
    void shouldNotReverseTransferWhenTargetNoLongerHasTheMoney() throws Exception {
        transfer("txn-1", "100.00", service()).andExpect(status().isOk());
        mockMvc.perform(post(MOVEMENT + "/service")
                .with(service())
                .header(IDEMPOTENCY_KEY, "txn-2")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountNumber\":\"" + TARGET + "\",\"amount\":100.00}"))
                .andExpect(status().isOk());

        reverse("txn-1").andExpect(status().isUnprocessableEntity());

        assertThat(fixtures.balanceOf(SOURCE)).isEqualByComparingTo("400.00");
        assertThat(movementRepository.findByIdempotencyKey("txn-1").orElseThrow().getStatus().name())
                .isEqualTo("COMPLETED");
    }

    @Test
    void shouldCancelKeyThatWasNeverAppliedSoALateRequestIsIgnored() throws Exception {
        reverse("txn-1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionStatus").value("CANCELLED"));

        transfer("txn-1", "100.00", service())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionStatus").value("CANCELLED"));

        assertThat(fixtures.balanceOf(SOURCE)).isEqualByComparingTo("500.00");
    }

    @Test
    void shouldRejectReversalFromCustomer() throws Exception {
        mockMvc.perform(post(MOVEMENT + "/{key}/reverse", "txn-1").with(customer("user-1")))
                .andExpect(status().isForbidden());
    }

    private ResultActions transfer(String key, String amount, RequestPostProcessor auth) throws Exception {
        return mockMvc.perform(post(MOVEMENT + "/transfer")
                .with(auth)
                .header(IDEMPOTENCY_KEY, key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(transferBody(amount)));
    }

    private void debit(String path, String key, String body) throws Exception {
        mockMvc.perform(post(MOVEMENT + path)
                .with(service())
                .header(IDEMPOTENCY_KEY, key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk());
    }

    private ResultActions reverse(String key) throws Exception {
        return mockMvc.perform(post(MOVEMENT + "/{key}/reverse", key).with(service()));
    }

    private static String transferBody(String amount) {
        return "{\"amount\":" + amount + ",\"sourceAccountNumber\":\"" + SOURCE
                + "\",\"targetAccountNumber\":\"" + TARGET + "\"}";
    }
}
