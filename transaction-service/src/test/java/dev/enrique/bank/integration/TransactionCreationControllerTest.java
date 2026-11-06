package dev.enrique.bank.integration;

import static dev.enrique.bank.commons.constants.PathConstants.TRANSACTION_CREATE;
import static dev.enrique.bank.util.JwtTestUtils.customer;
import static dev.enrique.bank.util.JwtTestUtils.tokenWithRole;
import static dev.enrique.bank.util.TransactionFixtures.CARD;
import static dev.enrique.bank.util.TransactionFixtures.CLABE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.enrique.bank.client.AccountClient;
import dev.enrique.bank.commons.dto.request.ClientPurchaseRequest;
import dev.enrique.bank.commons.dto.request.ClientServiceRequest;
import dev.enrique.bank.commons.dto.request.ClientTransferRequest;
import dev.enrique.bank.commons.dto.request.PurchaseRequest;
import dev.enrique.bank.commons.dto.request.ServiceRequest;
import dev.enrique.bank.commons.dto.request.ClientWithdrawalRequest;
import dev.enrique.bank.commons.dto.request.TransferRequest;
import dev.enrique.bank.commons.dto.request.WithdrawalRequest;
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
import dev.enrique.bank.util.PostgresContainerConfig;

/**
 * account-service is replaced by a mock of {@link AccountClient}: these tests check what
 * transaction-service sends to it and what it stores with the answer.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TransactionCreationControllerTest extends PostgresContainerConfig {
    private static final String TARGET_CLABE = "002180700148685530";
    private static final String ACCOUNT_NUMBER = "ACC-1700000000000";
    private static final MovementResultResponse COMPLETED =
            new MovementResultResponse(TransactionStatus.COMPLETED, "Valid Transaction");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private AccountClient accountClient;

    @BeforeEach
    void cleanUp() {
        transactionRepository.deleteAll();
    }

    // ---- transfer ----

    @Test
    void shouldRegisterCompletedTransfer() throws Exception {
        when(accountClient.processTransfer(any())).thenReturn(COMPLETED);

        mockMvc.perform(post(TRANSACTION_CREATE + "/transfer")
                .with(customer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildTransferRequest("250.00"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionCode").isNotEmpty())
                .andExpect(jsonPath("$.amount").value(250.00))
                .andExpect(jsonPath("$.transactionType").value("TRANSFER"))
                .andExpect(jsonPath("$.transactionStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.reason").value("Valid Transaction"))
                .andExpect(jsonPath("$.transactionDate").exists());

        verify(accountClient).processTransfer(
                new ClientTransferRequest(new BigDecimal("250.00"), CLABE, TARGET_CLABE));

        Transaction saved = singleSavedTransaction();
        assertThat(saved.getTransactionCode()).isNotBlank();
        assertThat(saved.getSourceIdentifier()).isEqualTo(CLABE);
        assertThat(saved.getIdentifierType()).isEqualTo(IdentifierType.CLABE);
        assertThat(saved.getAmount()).isEqualByComparingTo("250.00");
        assertThat(saved.getTransactionType()).isEqualTo(TransactionType.TRANSFER);
        assertThat(saved.getTransactionStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(saved.getTransferTransaction().getTargetIdentifier()).isEqualTo(TARGET_CLABE);
        assertThat(saved.getTransferTransaction().getTargetIdentifierType()).isEqualTo(IdentifierType.CLABE);
    }

    @Test
    void shouldRegisterDeclinedTransferWithReason() throws Exception {
        when(accountClient.processTransfer(any()))
                .thenReturn(new MovementResultResponse(TransactionStatus.DECLINED, "Insufficient funds"));

        mockMvc.perform(post(TRANSACTION_CREATE + "/transfer")
                .with(customer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildTransferRequest("250.00"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionStatus").value("DECLINED"))
                .andExpect(jsonPath("$.reason").value("Insufficient funds"));

        Transaction saved = singleSavedTransaction();
        assertThat(saved.getTransactionStatus()).isEqualTo(TransactionStatus.DECLINED);
        assertThat(saved.getReason()).isEqualTo("Insufficient funds");
    }

    @Test
    void shouldRejectInvalidTransferWithoutCallingAccountService() throws Exception {
        mockMvc.perform(post(TRANSACTION_CREATE + "/transfer")
                .with(customer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildTransferRequest("-10.00"))))
                .andExpect(status().isBadRequest());

        verify(accountClient, never()).processTransfer(any());
        assertThat(transactionRepository.count()).isZero();
    }

    @Test
    void shouldNotRecordTransactionWhenProcessingFailsUnexpectedly() {
        // The AccountClient fallback turns account-service errors into DECLINED/FAILED; anything
        // else is a bug and must roll back instead of being recorded.
        when(accountClient.processTransfer(any())).thenThrow(new IllegalStateException("boom"));

        assertThatThrownBy(() -> mockMvc.perform(post(TRANSACTION_CREATE + "/transfer")
                .with(customer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildTransferRequest("250.00")))))
                .hasRootCauseMessage("boom");

        assertThat(transactionRepository.count()).isZero();
    }

    @Test
    void shouldRejectTransferOfIncompleteRegistration() throws Exception {
        mockMvc.perform(post(TRANSACTION_CREATE + "/transfer")
                .with(tokenWithRole("CUSTOMER_BASIC", "PROFILE"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildTransferRequest("250.00"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(accountClient);
    }

    @Test
    void shouldRejectTransferWithoutToken() throws Exception {
        mockMvc.perform(post(TRANSACTION_CREATE + "/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildTransferRequest("250.00"))))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(accountClient);
    }

    // ---- service payment ----

    @Test
    void shouldRegisterServicePayment() throws Exception {
        when(accountClient.processService(any())).thenReturn(COMPLETED);

        ServiceRequest request = new ServiceRequest(new BigDecimal("875.50"), "Pago de CFE",
                ACCOUNT_NUMBER, "CFE-RPT-0099", ServiceType.ELECTRICITY, Currency.MX);

        mockMvc.perform(post(TRANSACTION_CREATE + "/service-payment")
                .with(customer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionType").value("SERVICE"))
                .andExpect(jsonPath("$.transactionStatus").value("COMPLETED"));

        verify(accountClient).processService(new ClientServiceRequest(ACCOUNT_NUMBER, new BigDecimal("875.50")));

        Transaction saved = singleSavedTransaction();
        assertThat(saved.getSourceIdentifier()).isEqualTo(ACCOUNT_NUMBER);
        assertThat(saved.getIdentifierType()).isEqualTo(IdentifierType.ACCOUNT);
        assertThat(saved.getTransactionType()).isEqualTo(TransactionType.SERVICE);
        assertThat(saved.getAmount()).isEqualByComparingTo("875.50");
        assertThat(saved.getServiceTransaction().getPaymentReference()).isEqualTo("CFE-RPT-0099");
        assertThat(saved.getServiceTransaction().getServiceType()).isEqualTo(ServiceType.ELECTRICITY);
    }

    // ---- purchase ----

    @Test
    void shouldRegisterPurchase() throws Exception {
        when(accountClient.processPurchase(any())).thenReturn(COMPLETED);

        PurchaseRequest request = new PurchaseRequest(new BigDecimal("349.99"), "Compra en tienda",
                ACCOUNT_NUMBER, CARD, "MERC-001", "RETAIL", "POS-001", "123", Currency.MX);

        mockMvc.perform(post(TRANSACTION_CREATE + "/purchase")
                .with(customer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionType").value("PURCHASE"))
                .andExpect(jsonPath("$.transactionStatus").value("COMPLETED"));

        verify(accountClient).processPurchase(
                new ClientPurchaseRequest(ACCOUNT_NUMBER, new BigDecimal("349.99"), CARD, "123"));

        Transaction saved = singleSavedTransaction();
        assertThat(saved.getSourceIdentifier()).isEqualTo(ACCOUNT_NUMBER);
        assertThat(saved.getIdentifierType()).isEqualTo(IdentifierType.ACCOUNT);
        assertThat(saved.getTransactionType()).isEqualTo(TransactionType.PURCHASE);
        assertThat(saved.getPurchaseTransaction().getSourceCardNumber()).isEqualTo(CARD);
        assertThat(saved.getPurchaseTransaction().getMerchantCode()).isEqualTo("MERC-001");
        assertThat(saved.getPurchaseTransaction().getPosId()).isEqualTo("POS-001");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM information_schema.columns "
                + "WHERE table_name = 'purchase_transaction' AND column_name = 'cvv'", Integer.class))
                .as("the cvv is never stored").isZero();
    }

    @Test
    void shouldRejectPurchaseWithoutSourceAccount() throws Exception {
        PurchaseRequest request = new PurchaseRequest(new BigDecimal("349.99"), "Compra en tienda",
                null, CARD, "MERC-001", "RETAIL", "POS-001", "123", Currency.MX);

        mockMvc.perform(post(TRANSACTION_CREATE + "/purchase")
                .with(customer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(accountClient);
    }

    // ---- withdrawal ----

    @Test
    void shouldRegisterWithdrawal() throws Exception {
        when(accountClient.processWithdrawal(any())).thenReturn(COMPLETED);

        WithdrawalRequest request = new WithdrawalRequest(new BigDecimal("1000.00"), "Retiro en cajero",
                ACCOUNT_NUMBER, "Av. Insurgentes Sur 1234", "ATM-SESSION-1", "RCP-1", null, null,
                new BigDecimal("25.00"), Currency.MX, SecurityVerificationMethod.PIN, WithdrawalMethod.DEBIT_CARD);

        mockMvc.perform(post(TRANSACTION_CREATE + "/withdrawal")
                .with(customer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionType").value("WITHDRAWAL"))
                .andExpect(jsonPath("$.transactionStatus").value("COMPLETED"));

        verify(accountClient).processWithdrawal(new ClientWithdrawalRequest(ACCOUNT_NUMBER,
                new BigDecimal("1000.00"), SecurityVerificationMethod.PIN, new BigDecimal("25.00")));

        Transaction saved = singleSavedTransaction();
        assertThat(saved.getSourceIdentifier()).isEqualTo(ACCOUNT_NUMBER);
        assertThat(saved.getTransactionType()).isEqualTo(TransactionType.WITHDRAWAL);
        assertThat(saved.getWithdrawalTransaction().getAtmLocation()).isEqualTo("Av. Insurgentes Sur 1234");
        assertThat(saved.getWithdrawalTransaction().getTransactionFee()).isEqualByComparingTo("25.00");
        assertThat(saved.getWithdrawalTransaction().getWithdrawalMethod()).isEqualTo(WithdrawalMethod.DEBIT_CARD);
    }

    private Transaction singleSavedTransaction() {
        List<Transaction> all = transactionRepository.findAll();
        assertThat(all).hasSize(1);
        return all.get(0);
    }

    private static TransferRequest buildTransferRequest(String amount) {
        return new TransferRequest(new BigDecimal(amount), "Pago de renta", Currency.MX,
                CLABE, IdentifierType.CLABE, TARGET_CLABE, IdentifierType.CLABE);
    }
}
