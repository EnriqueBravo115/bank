package dev.enrique.bank.integration;

import static dev.enrique.bank.util.JwtTestUtils.OTHER_USER;
import static dev.enrique.bank.util.JwtTestUtils.OWNER;
import static dev.enrique.bank.commons.constants.PathConstants.TRANSACTION_QUERY;
import static dev.enrique.bank.commons.enums.TransactionStatus.COMPLETED;
import static dev.enrique.bank.commons.enums.TransactionStatus.FAILED;
import static dev.enrique.bank.commons.enums.TransactionType.PURCHASE;
import static dev.enrique.bank.commons.enums.TransactionType.SERVICE;
import static dev.enrique.bank.commons.enums.TransactionType.TRANSFER;
import static dev.enrique.bank.util.JwtTestUtils.admin;
import static dev.enrique.bank.util.JwtTestUtils.customer;
import static dev.enrique.bank.util.JwtTestUtils.tokenWithRole;
import static dev.enrique.bank.util.TransactionFixtures.CARD;
import static dev.enrique.bank.util.TransactionFixtures.CARD_ACCOUNT;
import static dev.enrique.bank.util.TransactionFixtures.ACCOUNT;
import static dev.enrique.bank.util.TransactionFixtures.CLABE;
import static dev.enrique.bank.util.TransactionFixtures.transaction;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import dev.enrique.bank.client.AccountClient;
import dev.enrique.bank.commons.dto.response.AccountResponse;
import dev.enrique.bank.dao.TransactionRepository;
import dev.enrique.bank.util.FeignErrors;
import dev.enrique.bank.util.PostgresContainerConfig;

@SpringBootTest
@AutoConfigureMockMvc
class TransactionQueryControllerTest extends PostgresContainerConfig {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TransactionRepository transactionRepository;
    @MockitoBean
    private AccountClient accountClient;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        transactionRepository.saveAll(List.of(
                transaction("TXN-001", CLABE, TRANSFER, COMPLETED, "1500.00", "Pago de renta enero",
                        LocalDateTime.of(2024, 1, 15, 9, 30)),
                transaction("TXN-002", CLABE, SERVICE, COMPLETED, "300.00", "Pago de luz",
                        LocalDateTime.of(2024, 3, 10, 11, 0)),
                transaction("TXN-003", CLABE, TRANSFER, FAILED, "50.00", "Transferencia rechazada",
                        LocalDateTime.of(2023, 6, 1, 8, 0)),
                transaction("TXN-004", CARD, PURCHASE, COMPLETED, "200.00", "Compra en tienda",
                        LocalDateTime.of(2024, 2, 1, 18, 45))));
    }

    // ---- security ----

    @Test
    void history_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(TRANSACTION_QUERY + "/history")
                .param("sourceIdentifier", ACCOUNT)
                .param("status", "COMPLETED"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void history_withIncompleteRegistration_returns403() throws Exception {
        mockMvc.perform(get(TRANSACTION_QUERY + "/history")
                .param("sourceIdentifier", ACCOUNT)
                .param("status", "COMPLETED")
                .with(tokenWithRole("CUSTOMER_BASIC", "PROFILE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Registration incomplete"));
    }

    // ---- queries ----

    @Test
    void history_returnsTransactionsOfStatusNewestFirst() throws Exception {
        mockMvc.perform(get(TRANSACTION_QUERY + "/history")
                .param("sourceIdentifier", ACCOUNT)
                .param("status", "COMPLETED")
                .with(customer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].transactionCode").value("TXN-002"))
                .andExpect(jsonPath("$[1].transactionCode").value("TXN-001"))
                .andExpect(jsonPath("$[1].amount").value(1500.00))
                .andExpect(jsonPath("$[1].transactionType").value("TRANSFER"));

        mockMvc.perform(get(TRANSACTION_QUERY + "/history")
                .param("sourceIdentifier", ACCOUNT)
                .param("status", "FAILED")
                .with(customer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].transactionCode").value("TXN-003"));
    }

    @Test
    void all_paginatesTransactions() throws Exception {
        mockMvc.perform(get(TRANSACTION_QUERY + "/all")
                .param("sourceIdentifier", ACCOUNT)
                .param("status", "COMPLETED")
                .param("page", "0")
                .param("size", "1")
                .with(customer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].amount").value(300.00))
                .andExpect(jsonPath("$.content[0].transactionCode").value("TXN-002"))
                .andExpect(jsonPath("$.content[0].description").value("Pago de luz"))
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.size").value(1))
                .andExpect(jsonPath("$.page.totalElements").value(2))
                .andExpect(jsonPath("$.page.totalPages").value(2));
    }

    @Test
    void all_rejectsInvalidPageParams() throws Exception {
        mockMvc.perform(get(TRANSACTION_QUERY + "/all")
                .param("sourceIdentifier", ACCOUNT)
                .param("status", "COMPLETED")
                .param("size", "101")
                .with(customer()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(TRANSACTION_QUERY + "/all")
                .param("sourceIdentifier", ACCOUNT)
                .param("status", "COMPLETED")
                .param("page", "-1")
                .with(customer()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void statusYear_filtersByYearAndStatus() throws Exception {
        mockMvc.perform(get(TRANSACTION_QUERY + "/status-year")
                .param("sourceIdentifier", ACCOUNT)
                .param("status", "COMPLETED")
                .param("year", "2024")
                .with(customer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        // The only 2023 transaction is FAILED
        mockMvc.perform(get(TRANSACTION_QUERY + "/status-year")
                .param("sourceIdentifier", ACCOUNT)
                .param("status", "COMPLETED")
                .param("year", "2023")
                .with(customer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void byAccounts_asAdmin_returnsCompletedTransactionsOfAllAccounts() throws Exception {
        mockMvc.perform(get(TRANSACTION_QUERY + "/by-accounts")
                .param("accountNumbers", ACCOUNT, CARD_ACCOUNT)
                .with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    void byAccounts_asCustomer_returns403() throws Exception {
        mockMvc.perform(get(TRANSACTION_QUERY + "/by-accounts")
                .param("accountNumbers", ACCOUNT)
                .with(customer()))
                .andExpect(status().isForbidden());
    }

    @Test
    void max_returnsHighestAmountOfStatus() throws Exception {
        mockMvc.perform(get(TRANSACTION_QUERY + "/max")
                .param("sourceIdentifier", ACCOUNT)
                .param("status", "COMPLETED")
                .with(customer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(1500.00))
                .andExpect(jsonPath("$.transactionCode").value("TXN-001"));

        mockMvc.perform(get(TRANSACTION_QUERY + "/max")
                .param("sourceIdentifier", ACCOUNT)
                .param("status", "FAILED")
                .with(customer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(50.00))
                .andExpect(jsonPath("$.transactionCode").value("TXN-003"));
    }

    @Test
    void max_withoutTransactions_returns404() throws Exception {
        mockMvc.perform(get(TRANSACTION_QUERY + "/max")
                .param("sourceIdentifier", "000000000000000000")
                .param("status", "COMPLETED")
                .with(customer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("No COMPLETED transactions found for: 000000000000000000"));
    }

    @Test
    void dateRange_filtersByDate() throws Exception {
        mockMvc.perform(get(TRANSACTION_QUERY + "/date-range")
                .param("sourceIdentifier", ACCOUNT)
                .param("status", "COMPLETED")
                .param("startDate", "2024-01-01T00:00:00")
                .param("endDate", "2024-01-31T23:59:59")
                .with(customer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].amount").value(1500.00))
                .andExpect(jsonPath("$[0].transactionCode").value("TXN-001"));

        // The FAILED transfer of June 2023 is in range but has another status
        mockMvc.perform(get(TRANSACTION_QUERY + "/date-range")
                .param("sourceIdentifier", ACCOUNT)
                .param("status", "COMPLETED")
                .param("startDate", "2023-01-01T00:00:00")
                .param("endDate", "2023-12-31T23:59:59")
                .with(customer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get(TRANSACTION_QUERY + "/date-range")
                .param("sourceIdentifier", ACCOUNT)
                .param("status", "COMPLETED")
                .param("startDate", "2024-02-01T00:00:00")
                .param("endDate", "2024-02-29T23:59:59")
                .with(customer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void amountRange_filtersByAmountAndStatus() throws Exception {
        mockMvc.perform(get(TRANSACTION_QUERY + "/amount-range")
                .param("sourceIdentifier", ACCOUNT)
                .param("status", "COMPLETED")
                .param("minAmount", "100.00")
                .param("maxAmount", "2000.00")
                .with(customer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        // The 50.00 transfer is FAILED
        mockMvc.perform(get(TRANSACTION_QUERY + "/amount-range")
                .param("sourceIdentifier", ACCOUNT)
                .param("status", "COMPLETED")
                .param("minAmount", "1.00")
                .param("maxAmount", "100.00")
                .with(customer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void byTransactionCode_returnsTransaction() throws Exception {
        mockMvc.perform(get(TRANSACTION_QUERY + "/{transactionCode}", "TXN-001")
                .with(customer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionCode").value("TXN-001"))
                .andExpect(jsonPath("$.description").value("Pago de renta enero"))
                .andExpect(jsonPath("$.amount").value(1500.00))
                .andExpect(jsonPath("$.transactionStatus").value("COMPLETED"));
    }

    @Test
    void byTransactionCode_whenMissing_returns404() throws Exception {
        mockMvc.perform(get(TRANSACTION_QUERY + "/{transactionCode}", "TXN-999")
                .with(customer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Transaction not found with code: TXN-999"));
    }

    @Test
    void identifierType_filtersByType() throws Exception {
        mockMvc.perform(get(TRANSACTION_QUERY + "/identifier-type")
                .param("sourceIdentifier", ACCOUNT)
                .param("status", "COMPLETED")
                .param("identifierType", "CLABE")
                .with(customer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].transactionCode", containsInAnyOrder("TXN-001", "TXN-002")));

        mockMvc.perform(get(TRANSACTION_QUERY + "/identifier-type")
                .param("sourceIdentifier", ACCOUNT)
                .param("status", "COMPLETED")
                .param("identifierType", "CARD")
                .with(customer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void keyword_searchesDescriptionIgnoringCase() throws Exception {
        mockMvc.perform(get(TRANSACTION_QUERY + "/keyword")
                .param("sourceIdentifier", ACCOUNT)
                .param("status", "COMPLETED")
                .param("keyword", "RENTA")
                .with(customer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].description").value("Pago de renta enero"));
    }

    // ---- source identifier: account number or CLABE ----

    @Test
    void history_byAccountNumber_includesTransactionsCreatedWithAccountOrClabe() throws Exception {
        transactionRepository.save(transaction("TXN-005", ACCOUNT, TRANSFER, COMPLETED, "80.00",
                "Transferencia con numero de cuenta", LocalDateTime.of(2024, 4, 1, 10, 0)));

        mockMvc.perform(get(TRANSACTION_QUERY + "/history")
                .param("sourceIdentifier", ACCOUNT)
                .param("status", "COMPLETED")
                .with(customer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].transactionCode", containsInAnyOrder("TXN-005", "TXN-002", "TXN-001")));
        verifyNoInteractions(accountClient);
    }

    @Test
    void history_byClabe_resolvesTheAccountFirst() throws Exception {
        transactionRepository.save(transaction("TXN-005", ACCOUNT, TRANSFER, COMPLETED, "80.00",
                "Transferencia con numero de cuenta", LocalDateTime.of(2024, 4, 1, 10, 0)));
        when(accountClient.getByClabe(CLABE)).thenReturn(new AccountResponse(OWNER, ACCOUNT, CLABE, "OPEN"));

        mockMvc.perform(get(TRANSACTION_QUERY + "/history")
                .param("sourceIdentifier", CLABE)
                .param("sourceIdentifierType", "CLABE")
                .param("status", "COMPLETED")
                .with(customer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].transactionCode", containsInAnyOrder("TXN-005", "TXN-002", "TXN-001")));
    }

    @Test
    void identifierType_byClabe_filtersByTheIdentifierTheyWereCreatedWith() throws Exception {
        transactionRepository.save(transaction("TXN-005", ACCOUNT, TRANSFER, COMPLETED, "80.00",
                "Transferencia con numero de cuenta", LocalDateTime.of(2024, 4, 1, 10, 0)));
        when(accountClient.getByClabe(CLABE)).thenReturn(new AccountResponse(OWNER, ACCOUNT, CLABE, "OPEN"));

        mockMvc.perform(get(TRANSACTION_QUERY + "/identifier-type")
                .param("sourceIdentifier", CLABE)
                .param("sourceIdentifierType", "CLABE")
                .param("status", "COMPLETED")
                .param("identifierType", "ACCOUNT")
                .with(customer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].transactionCode").value("TXN-005"));
    }

    @Test
    void history_byUnknownClabe_returns404() throws Exception {
        when(accountClient.getByClabe("000000000000000000")).thenThrow(FeignErrors.status(404, ""));

        mockMvc.perform(get(TRANSACTION_QUERY + "/history")
                .param("sourceIdentifier", "000000000000000000")
                .param("sourceIdentifierType", "CLABE")
                .param("status", "COMPLETED")
                .with(customer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Account not found with CLABE: 000000000000000000"));
    }

    @Test
    void history_byClabe_whenAccountServiceIsDown_returns503() throws Exception {
        when(accountClient.getByClabe(CLABE)).thenThrow(FeignErrors.status(503, ""));

        mockMvc.perform(get(TRANSACTION_QUERY + "/history")
                .param("sourceIdentifier", CLABE)
                .param("sourceIdentifierType", "CLABE")
                .param("status", "COMPLETED")
                .with(customer()))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void history_byCard_resolvesTheAccountOfTheCard() throws Exception {
        when(accountClient.getByCardNumber(CARD))
                .thenReturn(new AccountResponse(OWNER, CARD_ACCOUNT, "646180110400000002", "OPEN"));

        mockMvc.perform(get(TRANSACTION_QUERY + "/history")
                .param("sourceIdentifier", CARD)
                .param("sourceIdentifierType", "CARD")
                .param("status", "COMPLETED")
                .with(customer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].transactionCode").value("TXN-004"));
    }

    @Test
    void history_byClabeOfAnotherUser_returnsNothing() throws Exception {
        when(accountClient.getByClabe(CLABE)).thenReturn(new AccountResponse(OWNER, ACCOUNT, CLABE, "OPEN"));

        mockMvc.perform(get(TRANSACTION_QUERY + "/history")
                .param("sourceIdentifier", CLABE)
                .param("sourceIdentifierType", "CLABE")
                .param("status", "COMPLETED")
                .with(customer(OTHER_USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ---- owner scope ----

    @Test
    void customer_onlySeesOwnTransactions_adminSeesAll() throws Exception {
        mockMvc.perform(get(TRANSACTION_QUERY + "/history")
                .param("sourceIdentifier", ACCOUNT)
                .param("status", "COMPLETED")
                .with(customer(OTHER_USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get(TRANSACTION_QUERY + "/{transactionCode}", "TXN-001").with(customer(OTHER_USER)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get(TRANSACTION_QUERY + "/history")
                .param("sourceIdentifier", ACCOUNT)
                .param("status", "COMPLETED")
                .with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
        mockMvc.perform(get(TRANSACTION_QUERY + "/{transactionCode}", "TXN-001").with(admin()))
                .andExpect(status().isOk());
    }
}
