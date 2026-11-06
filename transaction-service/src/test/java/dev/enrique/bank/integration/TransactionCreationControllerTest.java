package dev.enrique.bank.integration;

import static dev.enrique.bank.commons.constants.PathConstants.TRANSACTION_CREATE;
import static dev.enrique.bank.util.JwtTestUtils.customer;
import static dev.enrique.bank.util.JwtTestUtils.tokenWithRole;
import static dev.enrique.bank.util.TransactionFixtures.CARD;
import static dev.enrique.bank.util.TransactionFixtures.CLABE;
import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.enrique.bank.client.AccountClient;
import dev.enrique.bank.commons.dto.request.ClientServiceRequest;
import dev.enrique.bank.commons.dto.request.ClientTransferRequest;
import dev.enrique.bank.commons.dto.request.PurchaseRequest;
import dev.enrique.bank.commons.dto.request.ServiceRequest;
import dev.enrique.bank.commons.dto.request.TransferRequest;
import dev.enrique.bank.commons.dto.response.MovementResultResponse;
import dev.enrique.bank.commons.enums.Currency;
import dev.enrique.bank.commons.enums.IdentifierType;
import dev.enrique.bank.commons.enums.ServiceType;
import dev.enrique.bank.commons.enums.TransactionStatus;
import dev.enrique.bank.commons.enums.TransactionType;
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
    private static final MovementResultResponse COMPLETED =
            new MovementResultResponse(TransactionStatus.COMPLETED, "Valid Transaction");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private TransactionRepository transactionRepository;

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
                TransactionType.SERVICE, CLABE, "CFE-RPT-0099", ServiceType.ELECTRICITY, Currency.MX);

        mockMvc.perform(post(TRANSACTION_CREATE + "/service-payment")
                .with(customer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionType").value("SERVICE"))
                .andExpect(jsonPath("$.transactionStatus").value("COMPLETED"));

        verify(accountClient).processService(new ClientServiceRequest(CLABE, new BigDecimal("875.50")));

        Transaction saved = singleSavedTransaction();
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
                TransactionType.PURCHASE, CARD, "MERC-001", "RETAIL", "POS-001", "123", Currency.MX);

        mockMvc.perform(post(TRANSACTION_CREATE + "/purchase")
                .with(customer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionType").value("PURCHASE"))
                .andExpect(jsonPath("$.transactionStatus").value("COMPLETED"));

        Transaction saved = singleSavedTransaction();
        assertThat(saved.getTransactionType()).isEqualTo(TransactionType.PURCHASE);
        assertThat(saved.getPurchaseTransaction().getMerchantCode()).isEqualTo("MERC-001");
        assertThat(saved.getPurchaseTransaction().getPosId()).isEqualTo("POS-001");
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
