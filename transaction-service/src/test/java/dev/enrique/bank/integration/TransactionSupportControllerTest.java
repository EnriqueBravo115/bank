package dev.enrique.bank.integration;

import static dev.enrique.bank.commons.constants.PathConstants.TRANSACTION_SUPPORT;
import static dev.enrique.bank.commons.enums.TransactionStatus.COMPLETED;
import static dev.enrique.bank.commons.enums.TransactionStatus.FAILED;
import static dev.enrique.bank.commons.enums.TransactionType.SERVICE;
import static dev.enrique.bank.commons.enums.TransactionType.TRANSFER;
import static dev.enrique.bank.util.JwtTestUtils.customer;
import static dev.enrique.bank.util.TransactionFixtures.CLABE;
import static dev.enrique.bank.util.TransactionFixtures.transaction;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import dev.enrique.bank.dao.TransactionRepository;
import dev.enrique.bank.util.PostgresContainerConfig;

@SpringBootTest
@AutoConfigureMockMvc
class TransactionSupportControllerTest extends PostgresContainerConfig {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        transactionRepository.saveAll(List.of(
                transaction("TXN-001", CLABE, TRANSFER, COMPLETED, "1500.00", "Pago de renta enero",
                        LocalDateTime.of(2024, 1, 15, 9, 0)),
                transaction("TXN-002", CLABE, SERVICE, COMPLETED, "300.00", "Pago de luz",
                        LocalDateTime.of(2024, 2, 4, 9, 0)),
                transaction("TXN-003", CLABE, TRANSFER, FAILED, "50.00", "Retiro fallido",
                        LocalDateTime.of(2024, 3, 1, 9, 0))));
    }

    @Test
    void uniqueDescriptions_returnsUniqueWordsOfCompletedTransactions() throws Exception {
        mockMvc.perform(get(TRANSACTION_SUPPORT + "/unique-descriptions/{accountNumber}", CLABE)
                .with(customer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", containsInAnyOrder("Pago", "de", "renta", "enero", "luz")));
    }

    @Test
    void allDescriptions_joinsDescriptionsOfCompletedTransactions() throws Exception {
        mockMvc.perform(get(TRANSACTION_SUPPORT + "/all-descriptions/{accountNumber}", CLABE)
                .with(customer()))
                .andExpect(status().isOk())
                .andExpect(content().string(allOf(
                        containsString("Pago de renta enero"),
                        containsString("Pago de luz"),
                        not(containsString("Retiro fallido")))));
    }

    @Test
    void uniqueDescriptions_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(TRANSACTION_SUPPORT + "/unique-descriptions/{accountNumber}", CLABE))
                .andExpect(status().isUnauthorized());
    }
}
