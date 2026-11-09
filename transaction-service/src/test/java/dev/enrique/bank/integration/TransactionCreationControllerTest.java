package dev.enrique.bank.integration;

import static dev.enrique.bank.commons.constants.PathConstants.TRANSACTION;
import static dev.enrique.bank.commons.constants.PathConstants.TRANSACTION_CREATE;
import static dev.enrique.bank.util.JwtTestUtils.OTHER_USER;
import static dev.enrique.bank.util.JwtTestUtils.OWNER;
import static dev.enrique.bank.util.JwtTestUtils.admin;
import static dev.enrique.bank.util.JwtTestUtils.customer;
import static dev.enrique.bank.util.JwtTestUtils.tokenWithRole;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.enrique.bank.client.AccountClient;
import dev.enrique.bank.commons.dto.request.ClientDepositRequest;
import dev.enrique.bank.commons.dto.request.ClientPurchaseRequest;
import dev.enrique.bank.commons.dto.request.ClientServiceRequest;
import dev.enrique.bank.commons.dto.request.ClientTransferRequest;
import dev.enrique.bank.commons.dto.request.ClientWithdrawalRequest;
import dev.enrique.bank.commons.dto.request.DepositRequest;
import dev.enrique.bank.commons.dto.request.PurchaseRequest;
import dev.enrique.bank.commons.dto.request.ServiceRequest;
import dev.enrique.bank.commons.dto.request.TransferRequest;
import dev.enrique.bank.commons.dto.request.WithdrawalRequest;
import dev.enrique.bank.commons.dto.response.AccountResponse;
import dev.enrique.bank.commons.dto.response.MovementResultResponse;
import dev.enrique.bank.commons.enums.Currency;
import dev.enrique.bank.commons.enums.IdentifierType;
import dev.enrique.bank.commons.enums.SecurityVerificationMethod;
import dev.enrique.bank.commons.enums.ServiceType;
import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.commons.enums.TransactionType;
import dev.enrique.bank.commons.enums.WithdrawalMethod;
import dev.enrique.bank.dao.TransactionRepository;
import dev.enrique.bank.model.Transaction;
import dev.enrique.bank.settlement.TransactionSettlement;
import dev.enrique.bank.util.FeignErrors;
import dev.enrique.bank.util.PostgresContainerConfig;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * account-service is replaced by a mock of {@link AccountClient}: these tests check which accounts
 * are resolved, what is sent to account-service and what is stored with each answer.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TransactionCreationControllerTest extends PostgresContainerConfig {
    private static final String SOURCE_CLABE = "646180000000000001";
    private static final String SOURCE_ACCOUNT = "ACC-1";
    private static final String TARGET_CLABE = "646180000000000002";
    private static final String TARGET_ACCOUNT = "ACC-2";
    private static final String CARD = "4111111111111111";
    private static final MovementResultResponse COMPLETED =
            new MovementResultResponse(TransactionStatus.COMPLETED, "Valid Transaction");
    private static final MovementResultResponse UNKNOWN =
            new MovementResultResponse(TransactionStatus.PENDING, AccountClient.AWAITING_CONFIRMATION);

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private TransactionSettlement settlement;
    @Autowired
    private MeterRegistry meterRegistry;

    @MockitoBean
    private AccountClient accountClient;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        AccountResponse source = new AccountResponse(OWNER, SOURCE_ACCOUNT, SOURCE_CLABE, "OPEN");
        AccountResponse target = new AccountResponse(OTHER_USER, TARGET_ACCOUNT, TARGET_CLABE, "OPEN");
        when(accountClient.getByClabe(SOURCE_CLABE)).thenReturn(source);
        when(accountClient.getByAccountNumber(SOURCE_ACCOUNT)).thenReturn(source);
        when(accountClient.getByClabe(TARGET_CLABE)).thenReturn(target);
        when(accountClient.getByAccountNumber(TARGET_ACCOUNT)).thenReturn(target);
    }

    // ---- transfer ----

    @Test
    void shouldResolveAccountsAndRegisterCompletedTransfer() throws Exception {
        when(accountClient.processTransfer(anyString(), any())).thenReturn(COMPLETED);
        double completedAttempts = attempts("TRANSFER", "COMPLETED");

        transfer(SOURCE_CLABE, TARGET_CLABE, "250.00", customer())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(250.00))
                .andExpect(jsonPath("$.transactionType").value("TRANSFER"))
                .andExpect(jsonPath("$.transactionStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.reason").value("Valid Transaction"))
                .andExpect(jsonPath("$.transactionCode").isNotEmpty());

        Transaction saved = singleSavedTransaction();
        // The transaction code is the idempotency key; the CLABEs travel as account numbers.
        verify(accountClient).processTransfer(saved.getTransactionCode(),
                new ClientTransferRequest(new BigDecimal("250.00"), SOURCE_ACCOUNT, TARGET_ACCOUNT, Currency.MX));
        assertThat(saved.getSourceIdentifier()).isEqualTo(SOURCE_CLABE);
        assertThat(saved.getIdentifierType()).isEqualTo(IdentifierType.CLABE);
        assertThat(saved.getAccountNumber()).isEqualTo(SOURCE_ACCOUNT);
        assertThat(saved.getOwnerId()).isEqualTo(OWNER);
        // The receiver can find it too
        assertThat(saved.getCounterpartyAccountNumber()).isEqualTo(TARGET_ACCOUNT);
        assertThat(saved.getCounterpartyOwnerId()).isEqualTo(OTHER_USER);
        assertThat(saved.getTransactionStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(saved.getNextAttemptAt()).isNull();
        // Shown in Grafana ("Answers of account-service")
        assertThat(attempts("TRANSFER", "COMPLETED")).isEqualTo(completedAttempts + 1);
    }

    @Test
    void shouldRegisterDeclinedTransferWithReason() throws Exception {
        when(accountClient.processTransfer(anyString(), any()))
                .thenReturn(new MovementResultResponse(TransactionStatus.DECLINED, "Insufficient funds"));

        transfer(SOURCE_CLABE, TARGET_CLABE, "250.00", customer())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionStatus").value("DECLINED"))
                .andExpect(jsonPath("$.reason").value("Insufficient funds"));

        assertThat(singleSavedTransaction().getTransactionStatus()).isEqualTo(TransactionStatus.DECLINED);
    }

    @Test
    void shouldRejectTransferFromAccountOfAnotherUser() throws Exception {
        transfer(TARGET_CLABE, SOURCE_CLABE, "250.00", customer())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Account " + TARGET_CLABE + " does not belong to the user"));

        verify(accountClient, never()).processTransfer(anyString(), any());
        assertThat(transactionRepository.count()).isZero();
    }

    @Test
    void shouldLetAdminTransferFromAnyAccount() throws Exception {
        when(accountClient.processTransfer(anyString(), any())).thenReturn(COMPLETED);

        transfer(TARGET_CLABE, SOURCE_CLABE, "250.00", admin()).andExpect(status().isOk());

        assertThat(singleSavedTransaction().getOwnerId()).isEqualTo(OTHER_USER);
    }

    @Test
    void shouldResolveIdentifiersByTheirType() throws Exception {
        when(accountClient.processTransfer(anyString(), any())).thenReturn(COMPLETED);
        TransferRequest request = new TransferRequest(new BigDecimal("10.00"), "Payment", Currency.MX,
                SOURCE_ACCOUNT, IdentifierType.ACCOUNT, TARGET_CLABE, IdentifierType.CLABE);

        post(TRANSACTION_CREATE + "/transfer", request, customer()).andExpect(status().isOk());

        verify(accountClient).getByAccountNumber(SOURCE_ACCOUNT);
        verify(accountClient).getByClabe(TARGET_CLABE);
        verify(accountClient).processTransfer(anyString(),
                eq(new ClientTransferRequest(new BigDecimal("10.00"), SOURCE_ACCOUNT, TARGET_ACCOUNT, Currency.MX)));
    }

    @Test
    void shouldRejectTransferToUnknownAccount() throws Exception {
        when(accountClient.getByClabe("000000000000000000")).thenThrow(FeignErrors.status(404, "{}"));

        transfer(SOURCE_CLABE, "000000000000000000", "250.00", customer())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Account not found with CLABE: 000000000000000000"));

        assertThat(transactionRepository.count()).isZero();
    }

    @Test
    void shouldRejectTransferToTheSameAccount() throws Exception {
        transfer(SOURCE_CLABE, SOURCE_CLABE, "250.00", customer())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Source and target accounts must be different"));
    }

    @Test
    void shouldResolveCardIdentifiersToTheirAccount() throws Exception {
        when(accountClient.getByCardNumber(CARD))
                .thenReturn(new AccountResponse(OWNER, SOURCE_ACCOUNT, SOURCE_CLABE, "OPEN"));
        when(accountClient.processTransfer(anyString(), any())).thenReturn(COMPLETED);
        TransferRequest request = new TransferRequest(new BigDecimal("250.00"), "Payment", Currency.MX,
                CARD, IdentifierType.CARD, TARGET_CLABE, IdentifierType.CLABE);

        post(TRANSACTION_CREATE + "/transfer", request, customer())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionStatus").value("COMPLETED"));

        Transaction saved = singleSavedTransaction();
        verify(accountClient).processTransfer(saved.getTransactionCode(),
                new ClientTransferRequest(new BigDecimal("250.00"), SOURCE_ACCOUNT, TARGET_ACCOUNT, Currency.MX));
        assertThat(saved.getAccountNumber()).isEqualTo(SOURCE_ACCOUNT);
        assertThat(saved.getIdentifierType()).isEqualTo(IdentifierType.CARD);
    }

    @Test
    void shouldReturn404ForUnknownCard_withoutEchoingTheFullNumber() throws Exception {
        when(accountClient.getByCardNumber(CARD)).thenThrow(FeignErrors.status(404, ""));
        TransferRequest request = new TransferRequest(new BigDecimal("250.00"), "Payment", Currency.MX,
                CARD, IdentifierType.CARD, TARGET_CLABE, IdentifierType.CLABE);

        post(TRANSACTION_CREATE + "/transfer", request, customer())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Account not found with CARD: **** 1111"));
        assertThat(transactionRepository.count()).isZero();
    }

    @Test
    void shouldRejectCardOfAnotherUser_withoutEchoingTheFullNumber() throws Exception {
        when(accountClient.getByCardNumber(CARD))
                .thenReturn(new AccountResponse(OTHER_USER, TARGET_ACCOUNT, TARGET_CLABE, "OPEN"));
        TransferRequest request = new TransferRequest(new BigDecimal("250.00"), "Payment", Currency.MX,
                CARD, IdentifierType.CARD, SOURCE_CLABE, IdentifierType.CLABE);

        post(TRANSACTION_CREATE + "/transfer", request, customer())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Account **** 1111 does not belong to the user"));
    }

    @Test
    void shouldRefuseTransferWhenAccountsCannotBeChecked() throws Exception {
        when(accountClient.getByClabe(SOURCE_CLABE)).thenThrow(FeignErrors.status(503, ""));

        transfer(SOURCE_CLABE, TARGET_CLABE, "250.00", customer())
                .andExpect(status().isServiceUnavailable());

        assertThat(transactionRepository.count()).isZero();
    }

    @Test
    void shouldRejectInvalidTransferWithoutCallingAccountService() throws Exception {
        transfer(SOURCE_CLABE, TARGET_CLABE, "-10.00", customer()).andExpect(status().isBadRequest());

        verify(accountClient, never()).processTransfer(anyString(), any());
        assertThat(transactionRepository.count()).isZero();
    }

    @Test
    void shouldRejectTransferOfIncompleteRegistration() throws Exception {
        transfer(SOURCE_CLABE, TARGET_CLABE, "250.00", tokenWithRole("CUSTOMER_BASIC", "PROFILE"))
                .andExpect(status().isForbidden());

        verify(accountClient, never()).getByClabe(anyString());
    }

    @Test
    void shouldRejectTransferWithoutToken() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post(TRANSACTION_CREATE + "/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transferRequest(SOURCE_CLABE, TARGET_CLABE, "250.00"))))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(accountClient);
    }

    // ---- unknown outcome: PENDING and retried with the same key ----

    @Test
    void shouldKeepTransferPendingWhenOutcomeIsUnknownAndSettleItOnRetry() throws Exception {
        when(accountClient.processTransfer(anyString(), any())).thenReturn(UNKNOWN, COMPLETED);

        transfer(SOURCE_CLABE, TARGET_CLABE, "250.00", customer())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionStatus").value("PENDING"))
                .andExpect(jsonPath("$.reason").value(AccountClient.AWAITING_CONFIRMATION));

        Transaction pending = singleSavedTransaction();
        assertThat(pending.getTransactionStatus()).isEqualTo(TransactionStatus.PENDING);
        assertThat(pending.getAttempts()).isOne();
        assertThat(pending.getNextAttemptAt()).isAfter(LocalDateTime.now());

        // Not due yet: nothing is sent.
        assertThat(settlement.retryPending(LocalDateTime.now())).isZero();

        assertThat(settlement.retryPending(LocalDateTime.now().plusMinutes(1))).isOne();

        Transaction settled = singleSavedTransaction();
        assertThat(settled.getTransactionStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(settled.getNextAttemptAt()).isNull();
        verify(accountClient, times(2)).processTransfer(eq(pending.getTransactionCode()), any());
    }

    @Test
    void shouldBackOffBetweenAttemptsWhileOutcomeIsUnknown() throws Exception {
        when(accountClient.processTransfer(anyString(), any())).thenReturn(UNKNOWN);
        transfer(SOURCE_CLABE, TARGET_CLABE, "250.00", customer()).andExpect(status().isOk());
        LocalDateTime first = singleSavedTransaction().getNextAttemptAt();

        settlement.retryPending(first.plusSeconds(1));

        Transaction pending = singleSavedTransaction();
        assertThat(pending.getAttempts()).isEqualTo(2);
        assertThat(pending.getNextAttemptAt()).isAfterOrEqualTo(first.plusSeconds(10));
    }

    @Test
    void shouldStopRetryingAfterTheLastAttemptAndLetAnAdminRetryIt() throws Exception {
        when(accountClient.processTransfer(anyString(), any())).thenReturn(UNKNOWN);
        transfer(SOURCE_CLABE, TARGET_CLABE, "250.00", customer()).andExpect(status().isOk());
        Transaction pending = singleSavedTransaction();
        pending.setAttempts(TransactionSettlement.MAX_ATTEMPTS - 1);
        transactionRepository.save(pending);

        settlement.retryPending(LocalDateTime.now().plusHours(1));

        Transaction stuck = singleSavedTransaction();
        assertThat(meterRegistry.counter("bank.transaction.stuck", "type", "TRANSFER").count()).isPositive();
        assertThat(stuck.getTransactionStatus()).isEqualTo(TransactionStatus.PENDING);
        assertThat(stuck.getNextAttemptAt()).isNull();
        assertThat(stuck.getReason()).contains("after " + TransactionSettlement.MAX_ATTEMPTS + " attempts");
        // No more automatic attempts
        assertThat(settlement.retryPending(LocalDateTime.now().plusDays(1))).isZero();

        mockMvc.perform(MockMvcRequestBuilders.get(TRANSACTION + "/stuck").with(customer())).andExpect(status().isForbidden());
        mockMvc.perform(MockMvcRequestBuilders.get(TRANSACTION + "/stuck").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].transactionCode").value(stuck.getTransactionCode()))
                .andExpect(jsonPath("$[0].transactionStatus").value("PENDING"));

        when(accountClient.processTransfer(anyString(), any())).thenReturn(COMPLETED);
        lifecycle(stuck.getTransactionCode(), "/retry", customer()).andExpect(status().isForbidden());
        lifecycle(stuck.getTransactionCode(), "/retry", admin())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionStatus").value("COMPLETED"));

        mockMvc.perform(MockMvcRequestBuilders.get(TRANSACTION + "/stuck").with(admin())).andExpect(jsonPath("$", hasSize(0)));
        // Only stuck transactions can be retried by hand
        lifecycle(stuck.getTransactionCode(), "/retry", admin()).andExpect(status().isConflict());
    }

    @Test
    void stuckTransactionRetriedWithoutAnswer_getsANewRoundOfAttempts() throws Exception {
        when(accountClient.processTransfer(anyString(), any())).thenReturn(UNKNOWN);
        transfer(SOURCE_CLABE, TARGET_CLABE, "250.00", customer()).andExpect(status().isOk());
        Transaction pending = singleSavedTransaction();
        pending.setAttempts(TransactionSettlement.MAX_ATTEMPTS);
        pending.stopRetrying("stuck");
        transactionRepository.save(pending);

        lifecycle(pending.getTransactionCode(), "/retry", admin())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionStatus").value("PENDING"));

        Transaction retried = singleSavedTransaction();
        assertThat(retried.getAttempts()).isOne();
        assertThat(retried.getNextAttemptAt()).isNotNull();
    }

    @Test
    void shouldLeavePendingTransactionWhenProcessingFailsUnexpectedly() {
        // The fallback turns account-service errors into results; anything else propagates, and the
        // committed PENDING transaction is sent again later with the same key.
        when(accountClient.processTransfer(anyString(), any())).thenThrow(new IllegalStateException("boom"));

        assertThatThrownBy(() -> transfer(SOURCE_CLABE, TARGET_CLABE, "250.00", customer()))
                .hasRootCauseMessage("boom");

        assertThat(singleSavedTransaction().getTransactionStatus()).isEqualTo(TransactionStatus.PENDING);
    }

    // ---- service payment, purchase, withdrawal ----

    @Test
    void shouldRegisterServicePayment() throws Exception {
        when(accountClient.processService(anyString(), any())).thenReturn(COMPLETED);

        ServiceRequest request = new ServiceRequest(new BigDecimal("875.50"), "Electricity bill payment",
                SOURCE_ACCOUNT, "CFE-RPT-0099", ServiceType.ELECTRICITY, Currency.MX);

        post(TRANSACTION_CREATE + "/service-payment", request, customer())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionType").value("SERVICE"))
                .andExpect(jsonPath("$.transactionStatus").value("COMPLETED"));

        Transaction saved = singleSavedTransaction();
        verify(accountClient).processService(saved.getTransactionCode(),
                new ClientServiceRequest(SOURCE_ACCOUNT, new BigDecimal("875.50"), Currency.MX));
        assertThat(saved.getIdentifierType()).isEqualTo(IdentifierType.ACCOUNT);
        assertThat(saved.getOwnerId()).isEqualTo(OWNER);
    }

    @Test
    void shouldRejectServicePaymentFromAccountOfAnotherUser() throws Exception {
        ServiceRequest request = new ServiceRequest(new BigDecimal("875.50"), "Electricity bill payment",
                TARGET_ACCOUNT, "CFE-RPT-0099", ServiceType.ELECTRICITY, Currency.MX);

        post(TRANSACTION_CREATE + "/service-payment", request, customer()).andExpect(status().isForbidden());

        verify(accountClient, never()).processService(anyString(), any());
    }

    @Test
    void shouldRegisterPurchaseSendingTheCvvOnEveryAttempt() throws Exception {
        when(accountClient.processPurchase(anyString(), any())).thenReturn(UNKNOWN, COMPLETED);

        PurchaseRequest request = new PurchaseRequest(new BigDecimal("349.99"), "Store purchase",
                SOURCE_ACCOUNT, CARD, "MERC-001", "RETAIL", "POS-001", "123", Currency.MX);

        post(TRANSACTION_CREATE + "/purchase", request, customer())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionType").value("PURCHASE"));
        settlement.retryPending(LocalDateTime.now().plusMinutes(1));

        Transaction saved = singleSavedTransaction();
        verify(accountClient, times(2)).processPurchase(saved.getTransactionCode(),
                new ClientPurchaseRequest(SOURCE_ACCOUNT, new BigDecimal("349.99"), CARD, "123", Currency.MX));
        assertThat(saved.getTransactionStatus()).isEqualTo(TransactionStatus.COMPLETED);
    }

    @Test
    void shouldRejectPurchaseWithoutSourceAccount() throws Exception {
        PurchaseRequest request = new PurchaseRequest(new BigDecimal("349.99"), "Store purchase",
                null, CARD, "MERC-001", "RETAIL", "POS-001", "123", Currency.MX);

        post(TRANSACTION_CREATE + "/purchase", request, customer()).andExpect(status().isBadRequest());

        verifyNoInteractions(accountClient);
    }

    @Test
    void shouldRegisterWithdrawal() throws Exception {
        when(accountClient.processWithdrawal(anyString(), any())).thenReturn(COMPLETED);

        WithdrawalRequest request = new WithdrawalRequest(new BigDecimal("1000.00"), "ATM withdrawal",
                SOURCE_ACCOUNT, "Av. Insurgentes Sur 1234", "ATM-SESSION-1", "RCP-1", null, null,
                new BigDecimal("25.00"), Currency.MX, SecurityVerificationMethod.PIN, WithdrawalMethod.DEBIT_CARD);

        post(TRANSACTION_CREATE + "/withdrawal", request, customer())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionType").value("WITHDRAWAL"))
                .andExpect(jsonPath("$.transactionStatus").value("COMPLETED"));

        Transaction saved = singleSavedTransaction();
        verify(accountClient).processWithdrawal(saved.getTransactionCode(), new ClientWithdrawalRequest(
                SOURCE_ACCOUNT, new BigDecimal("1000.00"), SecurityVerificationMethod.PIN, new BigDecimal("25.00"), Currency.MX));
        assertThat(saved.getTransactionType()).isEqualTo(TransactionType.WITHDRAWAL);
    }

    // ---- deposit ----

    @Test
    void shouldLetAdminDepositIntoAnyAccount_asATransactionOfItsOwner() throws Exception {
        when(accountClient.processDeposit(anyString(), any())).thenReturn(COMPLETED);

        DepositRequest request = new DepositRequest(new BigDecimal("1500.00"), "Cash deposit at branch",
                TARGET_CLABE, IdentifierType.CLABE, Currency.MX);
        post(TRANSACTION_CREATE + "/deposit", request, admin())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionType").value("DEPOSIT"))
                .andExpect(jsonPath("$.transactionStatus").value("COMPLETED"));

        Transaction saved = singleSavedTransaction();
        verify(accountClient).processDeposit(saved.getTransactionCode(),
                new ClientDepositRequest(TARGET_ACCOUNT, new BigDecimal("1500.00"), Currency.MX));
        assertThat(saved.getAccountNumber()).isEqualTo(TARGET_ACCOUNT);
        assertThat(saved.getOwnerId()).isEqualTo(OTHER_USER);
        assertThat(saved.getSourceIdentifier()).isEqualTo(TARGET_CLABE);
    }

    @Test
    void shouldNotLetCustomersDeposit() throws Exception {
        DepositRequest request = new DepositRequest(new BigDecimal("1500.00"), "Cash deposit",
                SOURCE_ACCOUNT, IdentifierType.ACCOUNT, Currency.MX);

        post(TRANSACTION_CREATE + "/deposit", request, customer()).andExpect(status().isForbidden());
        post(TRANSACTION_CREATE + "/deposit", new DepositRequest(BigDecimal.ZERO, "Cash deposit",
                SOURCE_ACCOUNT, IdentifierType.ACCOUNT, Currency.MX), admin()).andExpect(status().isBadRequest());

        verifyNoInteractions(accountClient);
    }

    // ---- cancel and reverse ----

    @Test
    void shouldCancelPendingTransaction() throws Exception {
        when(accountClient.processTransfer(anyString(), any())).thenReturn(UNKNOWN);
        when(accountClient.reverse(anyString()))
                .thenReturn(new MovementResultResponse(TransactionStatus.CANCELLED, "Cancelled before being applied"));
        transfer(SOURCE_CLABE, TARGET_CLABE, "250.00", customer());
        String code = singleSavedTransaction().getTransactionCode();

        lifecycle(code, "/cancel", customer())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionStatus").value("CANCELLED"));

        verify(accountClient).reverse(code);
        assertThat(singleSavedTransaction().getTransactionStatus()).isEqualTo(TransactionStatus.CANCELLED);
        // A cancelled transaction is no longer retried.
        assertThat(settlement.retryPending(LocalDateTime.now().plusHours(1))).isZero();
    }

    @Test
    void shouldNotCancelTransactionOfAnotherUserOrThatIsNotPending() throws Exception {
        when(accountClient.processTransfer(anyString(), any())).thenReturn(UNKNOWN, COMPLETED);
        transfer(SOURCE_CLABE, TARGET_CLABE, "250.00", customer());
        String code = singleSavedTransaction().getTransactionCode();

        lifecycle(code, "/cancel", customer(OTHER_USER)).andExpect(status().isNotFound());

        settlement.retryPending(LocalDateTime.now().plusMinutes(1));
        lifecycle(code, "/cancel", customer())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Only PENDING transactions can be cancelled, this one is COMPLETED"));
        verify(accountClient, never()).reverse(anyString());
    }

    @Test
    void shouldLetOnlyAdminsReverseCompletedTransactions() throws Exception {
        when(accountClient.processTransfer(anyString(), any())).thenReturn(COMPLETED);
        when(accountClient.reverse(anyString()))
                .thenReturn(new MovementResultResponse(TransactionStatus.REVERSED, "Reversed"));
        transfer(SOURCE_CLABE, TARGET_CLABE, "250.00", customer());
        String code = singleSavedTransaction().getTransactionCode();

        lifecycle(code, "/reverse", customer()).andExpect(status().isForbidden());

        lifecycle(code, "/reverse", admin())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionStatus").value("REVERSED"));
        assertThat(singleSavedTransaction().getTransactionStatus()).isEqualTo(TransactionStatus.REVERSED);

        lifecycle(code, "/reverse", admin()).andExpect(status().isConflict());
    }

    @Test
    void shouldKeepTransactionWhenAccountServiceCannotUndoIt() throws Exception {
        when(accountClient.processTransfer(anyString(), any())).thenReturn(COMPLETED);
        transfer(SOURCE_CLABE, TARGET_CLABE, "250.00", customer());
        String code = singleSavedTransaction().getTransactionCode();

        when(accountClient.reverse(code))
                .thenReturn(new MovementResultResponse(TransactionStatus.DECLINED, "Insufficient funds"));
        lifecycle(code, "/reverse", admin())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("account-service could not undo the transaction: Insufficient funds"));

        when(accountClient.reverse(code)).thenReturn(UNKNOWN);
        lifecycle(code, "/reverse", admin()).andExpect(status().isServiceUnavailable());

        assertThat(singleSavedTransaction().getTransactionStatus()).isEqualTo(TransactionStatus.COMPLETED);
    }

    @Test
    void shouldReturn404WhenUndoingUnknownTransaction() throws Exception {
        lifecycle("missing", "/reverse", admin())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Transaction not found with code: missing"));
    }

    private Transaction singleSavedTransaction() {
        List<Transaction> all = transactionRepository.findAll();
        assertThat(all).hasSize(1);
        return all.get(0);
    }

    private ResultActions transfer(String source, String target, String amount, RequestPostProcessor auth)
            throws Exception {
        return post(TRANSACTION_CREATE + "/transfer", transferRequest(source, target, amount), auth);
    }

    private double attempts(String type, String status) {
        return meterRegistry.counter("bank.transaction.attempts", "type", type, "status", status).count();
    }

    private ResultActions lifecycle(String code, String action, RequestPostProcessor auth) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post(TRANSACTION + "/{code}" + action, code).with(auth));
    }

    private ResultActions post(String path, Object body, RequestPostProcessor auth) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post(path)
                .with(auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private static TransferRequest transferRequest(String source, String target, String amount) {
        return new TransferRequest(new BigDecimal(amount), "Rent payment", Currency.MX,
                source, IdentifierType.CLABE, target, IdentifierType.CLABE);
    }
}
