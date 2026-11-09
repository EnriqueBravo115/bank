package dev.enrique.bank.integration;

import static dev.enrique.bank.commons.constants.PathConstants.LIMIT;
import static dev.enrique.bank.util.JwtTestUtils.admin;
import static dev.enrique.bank.util.JwtTestUtils.customer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.enrique.bank.commons.enums.AccountStatus;
import dev.enrique.bank.commons.enums.Currency;
import dev.enrique.bank.dao.TransactionLimitRepository;
import dev.enrique.bank.util.AccountFixtures;
import dev.enrique.bank.util.PostgresContainerConfig;

@SpringBootTest
@AutoConfigureMockMvc
class LimitControllerTest extends PostgresContainerConfig {
    private static final String ACCOUNT_NUMBER = "ACC-1";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AccountFixtures fixtures;
    @Autowired
    private TransactionLimitRepository transactionLimitRepository;
    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        fixtures.clear();
        fixtures.account("user-1", ACCOUNT_NUMBER, "646180000000000001", AccountStatus.OPEN, "500.00", Currency.US);
    }

    @Test
    void save_createsTheLimitInTheAccountCurrency_andReplacesItOnTheNextSave() throws Exception {
        save(ACCOUNT_NUMBER, "TRANSFER", "DAILY", "1000.00", 5, admin())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value(ACCOUNT_NUMBER))
                .andExpect(jsonPath("$.limitType").value("TRANSFER"))
                .andExpect(jsonPath("$.timePeriod").value("DAILY"))
                .andExpect(jsonPath("$.maxAmount").value(1000.00))
                .andExpect(jsonPath("$.maxTransactions").value(5))
                .andExpect(jsonPath("$.currency").value("US"));
        save(ACCOUNT_NUMBER, "TRANSFER", "DAILY", "2000.00", 10, admin())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxAmount").value(2000.00));
        save(ACCOUNT_NUMBER, "TRANSFER", "MONTHLY", "20000.00", 100, admin()).andExpect(status().isOk());

        assertThat(transactionLimitRepository.count()).isEqualTo(2);
    }

    @Test
    void save_validatesTheRequest() throws Exception {
        save(ACCOUNT_NUMBER, "TRANSFER", "DAILY", "0", 5, admin()).andExpect(status().isBadRequest());
        save(ACCOUNT_NUMBER, "TRANSFER", "DAILY", "100.00", 0, admin()).andExpect(status().isBadRequest());
        save("ACC-404", "TRANSFER", "DAILY", "100.00", 5, admin()).andExpect(status().isNotFound());

        assertThat(transactionLimitRepository.count()).isZero();
    }

    @Test
    void save_isOnlyForAdmins() throws Exception {
        save(ACCOUNT_NUMBER, "TRANSFER", "DAILY", "1000000.00", 1000, customer("user-1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerSeesOnlyTheLimitsOfTheirAccount() throws Exception {
        fixtures.account("user-2", "ACC-2", "646180000000000002", AccountStatus.OPEN, "0.00");
        save(ACCOUNT_NUMBER, "PURCHASE", "WEEKLY", "300.00", 20, admin());
        save("ACC-2", "PURCHASE", "WEEKLY", "300.00", 20, admin());

        mockMvc.perform(get(LIMIT + "/me").with(customer("user-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].accountNumber").value(ACCOUNT_NUMBER));
        mockMvc.perform(get(LIMIT + "/{accountNumber}", "ACC-2").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get(LIMIT + "/{accountNumber}", "ACC-2").with(customer("user-1")))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_removesTheLimit() throws Exception {
        long id = objectMapper.readTree(save(ACCOUNT_NUMBER, "SERVICE", "DAILY", "100.00", 3, admin())
                .andReturn().getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(delete(LIMIT + "/{id}", id).with(customer("user-1"))).andExpect(status().isForbidden());
        mockMvc.perform(delete(LIMIT + "/{id}", id).with(admin())).andExpect(status().isNoContent());
        mockMvc.perform(delete(LIMIT + "/{id}", id).with(admin())).andExpect(status().isNotFound());

        assertThat(transactionLimitRepository.count()).isZero();
    }

    private ResultActions save(String accountNumber, String type, String period, String maxAmount,
            int maxTransactions, RequestPostProcessor auth) throws Exception {
        return mockMvc.perform(put(LIMIT)
                .with(auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountNumber\":\"" + accountNumber + "\",\"limitType\":\"" + type
                        + "\",\"timePeriod\":\"" + period + "\",\"maxAmount\":" + maxAmount
                        + ",\"maxTransactions\":" + maxTransactions + "}"));
    }
}
