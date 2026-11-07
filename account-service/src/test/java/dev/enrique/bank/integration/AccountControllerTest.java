package dev.enrique.bank.integration;

import static dev.enrique.bank.commons.constants.PathConstants.ACCOUNT;
import static dev.enrique.bank.util.JwtTestUtils.admin;
import static dev.enrique.bank.util.JwtTestUtils.customer;
import static dev.enrique.bank.util.JwtTestUtils.service;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import dev.enrique.bank.commons.enums.AccountStatus;
import dev.enrique.bank.util.AccountFixtures;
import dev.enrique.bank.util.PostgresContainerConfig;

@SpringBootTest
@AutoConfigureMockMvc
class AccountControllerTest extends PostgresContainerConfig {
    private static final String ACCOUNT_NUMBER = "ACC-1";
    private static final String CLABE = "646180000000000001";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AccountFixtures fixtures;

    @BeforeEach
    void setUp() {
        fixtures.clear();
        fixtures.account("user-1", ACCOUNT_NUMBER, CLABE, AccountStatus.OPEN, "500.00");
    }

    @Test
    void getMe_returnsAccountOfJwtSubject() throws Exception {
        mockMvc.perform(get(ACCOUNT + "/me").with(customer("user-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value(ACCOUNT_NUMBER))
                .andExpect(jsonPath("$.userId").value("user-1"));
    }

    @Test
    void getMe_withoutAccount_returns404() throws Exception {
        mockMvc.perform(get(ACCOUNT + "/me").with(customer("user-9")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Account not found with userId: user-9"));
    }

    @Test
    void getMe_withIncompleteRegistration_returns403() throws Exception {
        mockMvc.perform(get(ACCOUNT + "/me").with(jwt()
                .jwt(token -> token.subject("user-1").claim("registration_status", "PROFILE"))
                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER_BASIC"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Registration incomplete"));
    }

    @Test
    void lookups_asCustomer_return403() throws Exception {
        for (String path : new String[] {
                "/get-by-account-number/" + ACCOUNT_NUMBER,
                "/get-by-clabe/" + CLABE,
                "/get-by-email/user-1@test.com" }) {
            mockMvc.perform(get(ACCOUNT + path).with(customer("user-1"))).andExpect(status().isForbidden());
        }
    }

    @Test
    void lookups_asService_resolveAccountAndOwner() throws Exception {
        mockMvc.perform(get(ACCOUNT + "/get-by-account-number/{n}", ACCOUNT_NUMBER).with(service()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user-1"));
        mockMvc.perform(get(ACCOUNT + "/get-by-clabe/{c}", CLABE).with(service()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value(ACCOUNT_NUMBER));
    }

    @Test
    void lookups_asAdmin_returnAccount() throws Exception {
        mockMvc.perform(get(ACCOUNT + "/get-by-email/{e}", "user-1@test.com").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value(ACCOUNT_NUMBER));
    }

    @Test
    void lookups_whenMissing_return404WithTheField() throws Exception {
        mockMvc.perform(get(ACCOUNT + "/get-by-clabe/{c}", "000000000000000000").with(admin()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Account not found with clabe: 000000000000000000"));
        mockMvc.perform(get(ACCOUNT + "/get-by-email/{e}", "nobody@test.com").with(admin()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Account not found with email: nobody@test.com"));
    }
}
