package dev.enrique.bank.integration;

import static dev.enrique.bank.commons.constants.PathConstants.TRANSACTION_SUPPORT;
import static dev.enrique.bank.util.JwtTestUtils.customer;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import dev.enrique.bank.util.PostgresContainerConfig;

/**
 * Uses the seed data from db/migration/V2__populate_transactions.sql.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TransactionSupportControllerTest extends PostgresContainerConfig {
    private static final String CLABE = "646180110400000001";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void uniqueDescriptions_returnsUniqueWords() throws Exception {
        mockMvc.perform(get(TRANSACTION_SUPPORT + "/unique-descriptions/{accountNumber}", CLABE)
                .with(customer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", containsInAnyOrder("Pago", "de", "renta", "enero")));
    }

    @Test
    void allDescriptions_returnsDescriptions() throws Exception {
        mockMvc.perform(get(TRANSACTION_SUPPORT + "/all-descriptions/{accountNumber}", CLABE)
                .with(customer()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Pago de renta enero")));
    }

    @Test
    void uniqueDescriptions_withoutToken_returns401() throws Exception {
        mockMvc.perform(get(TRANSACTION_SUPPORT + "/unique-descriptions/{accountNumber}", CLABE))
                .andExpect(status().isUnauthorized());
    }
}
