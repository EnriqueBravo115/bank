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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import dev.enrique.bank.commons.dto.request.ClientServiceRequest;
import dev.enrique.bank.commons.dto.request.ClientTransferRequest;
import dev.enrique.bank.commons.enums.AccountStatus;
import dev.enrique.bank.commons.enums.CardStatus;
import dev.enrique.bank.commons.exception.InsufficientFundsException;
import dev.enrique.bank.commons.util.CardSecurityCode;
import dev.enrique.bank.dao.MovementRepository;
import dev.enrique.bank.model.Account;
import dev.enrique.bank.service.MovementService;
import dev.enrique.bank.util.AccountFixtures;
import dev.enrique.bank.util.PostgresContainerConfig;

@SpringBootTest
@AutoConfigureMockMvc
class MovementControllerTest extends PostgresContainerConfig {
    private static final String SOURCE = "ACC-1";
    private static final String TARGET = "ACC-2";
    private static final String CARD = "4123450000000001";
    private static final LocalDate CARD_EXPIRATION = LocalDate.of(2030, 12, 31);

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AccountFixtures fixtures;
    @Autowired
    private MovementRepository movementRepository;
    @Autowired
    private MovementService movementService;
    @Autowired
    private CardSecurityCode cardSecurityCode;

    private Account source;
    private Account target;

    @BeforeEach
    void setUp() {
        fixtures.clear();
        source = fixtures.account("user-1", SOURCE, "646180000000000001", AccountStatus.OPEN, "500.00");
        target = fixtures.account("user-2", TARGET, "646180000000000002", AccountStatus.OPEN, "0.00");
        fixtures.card(source, CARD, CardStatus.ACTIVE, CARD_EXPIRATION);
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
        debit("/purchase", "txn-p", purchaseBody(CARD, cvv()));
        debit("/purchase", "txn-p", purchaseBody(CARD, cvv()));
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

    // ---- purchases: card and security code ----

    @Test
    void shouldRequireTheCardInPurchases() throws Exception {
        purchase("txn-p", "{\"accountNumber\":\"" + SOURCE + "\",\"amount\":10.00}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldDeclinePurchaseWithWrongOrMissingSecurityCode() throws Exception {
        String wrong = cvv().equals("000") ? "001" : "000";
        purchase("txn-p", purchaseBody(CARD, wrong))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Card declined: invalid security code"));
        purchase("txn-p", purchaseBody(CARD, null))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Card declined: invalid security code"));

        assertThat(fixtures.balanceOf(SOURCE)).isEqualByComparingTo("500.00");
        assertThat(movementRepository.count()).isZero();
    }

    @Test
    void shouldDeclinePurchaseWithCardOfAnotherAccount() throws Exception {
        fixtures.card(target, "4123450000000019", CardStatus.ACTIVE, CARD_EXPIRATION);

        purchase("txn-p", purchaseBody("4123450000000019",
                cardSecurityCode.of("4123450000000019", CARD_EXPIRATION)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Card declined: the card is not linked to the account"));
        purchase("txn-p", purchaseBody("4000000000000002", "123"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Card declined: the card is not linked to the account"));
    }

    @Test
    void shouldDeclinePurchaseWithBlockedOrExpiredCard() throws Exception {
        fixtures.clear();
        source = fixtures.account("user-1", SOURCE, "646180000000000001", AccountStatus.OPEN, "500.00");
        fixtures.card(source, CARD, CardStatus.BLOCKED, CARD_EXPIRATION);
        LocalDate expired = LocalDate.of(2020, 1, 31);
        fixtures.card(source, "4123450000000027", CardStatus.ACTIVE, expired);

        purchase("txn-p", purchaseBody(CARD, cvv()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Card declined: the card is BLOCKED"));
        purchase("txn-p", purchaseBody("4123450000000027", cardSecurityCode.of("4123450000000027", expired)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Card declined: the card is expired"));

        assertThat(fixtures.balanceOf(SOURCE)).isEqualByComparingTo("500.00");
    }

    // ---- concurrency ----

    @Test
    void shouldNotSpendTheSameBalanceTwiceWithConcurrentDebits() throws Exception {
        // 10 debits of 100.00 at once from a balance of 500.00: exactly 5 must succeed.
        int requests = 10;
        ExecutorService executor = Executors.newFixedThreadPool(requests);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        for (int i = 0; i < requests; i++) {
            String key = "txn-" + i;
            results.add(executor.submit(() -> {
                start.await();
                try {
                    movementService.processService(key, new ClientServiceRequest(SOURCE, new BigDecimal("100.00")));
                    return true;
                } catch (InsufficientFundsException ex) {
                    return false;
                }
            }));
        }
        start.countDown();

        int completed = 0;
        for (Future<Boolean> result : results) {
            completed += result.get(30, TimeUnit.SECONDS) ? 1 : 0;
        }
        executor.shutdown();

        assertThat(completed).isEqualTo(5);
        assertThat(fixtures.balanceOf(SOURCE)).isEqualByComparingTo("0.00");
        assertThat(movementRepository.count()).isEqualTo(5);
    }

    @Test
    void shouldNotDeadlockWithOppositeConcurrentTransfers() throws Exception {
        fixtures.clear();
        fixtures.account("user-1", SOURCE, "646180000000000001", AccountStatus.OPEN, "500.00");
        fixtures.account("user-2", TARGET, "646180000000000002", AccountStatus.OPEN, "500.00");

        int requests = 10;
        ExecutorService executor = Executors.newFixedThreadPool(requests);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> results = new ArrayList<>();
        for (int i = 0; i < requests; i++) {
            boolean forward = i % 2 == 0;
            String key = "txn-" + i;
            results.add(executor.submit(() -> {
                start.await();
                return movementService.processTransfer(key, new ClientTransferRequest(new BigDecimal("10.00"),
                        forward ? SOURCE : TARGET, forward ? TARGET : SOURCE));
            }));
        }
        start.countDown();
        for (Future<?> result : results) {
            result.get(30, TimeUnit.SECONDS);
        }
        executor.shutdown();

        assertThat(fixtures.balanceOf(SOURCE)).isEqualByComparingTo("500.00");
        assertThat(fixtures.balanceOf(TARGET)).isEqualByComparingTo("500.00");
        assertThat(movementRepository.count()).isEqualTo(requests);
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

    private ResultActions purchase(String key, String body) throws Exception {
        return mockMvc.perform(post(MOVEMENT + "/purchase")
                .with(service())
                .header(IDEMPOTENCY_KEY, key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private String cvv() {
        return cardSecurityCode.of(CARD, CARD_EXPIRATION);
    }

    private static String purchaseBody(String cardNumber, String cvv) {
        return "{\"accountNumber\":\"" + SOURCE + "\",\"amount\":10.00,\"sourceCardNumber\":\"" + cardNumber
                + "\"" + (cvv == null ? "" : ",\"cvv\":\"" + cvv + "\"") + "}";
    }

    private ResultActions reverse(String key) throws Exception {
        return mockMvc.perform(post(MOVEMENT + "/{key}/reverse", key).with(service()));
    }

    private static String transferBody(String amount) {
        return "{\"amount\":" + amount + ",\"sourceAccountNumber\":\"" + SOURCE
                + "\",\"targetAccountNumber\":\"" + TARGET + "\"}";
    }
}
