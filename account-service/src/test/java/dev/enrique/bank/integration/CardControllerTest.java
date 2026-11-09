package dev.enrique.bank.integration;

import static dev.enrique.bank.commons.constants.Headers.IDEMPOTENCY_KEY;
import static dev.enrique.bank.commons.constants.PathConstants.ACCOUNT;
import static dev.enrique.bank.commons.constants.PathConstants.CARD;
import static dev.enrique.bank.commons.constants.PathConstants.MOVEMENT;
import static dev.enrique.bank.util.JwtTestUtils.admin;
import static dev.enrique.bank.util.JwtTestUtils.customer;
import static dev.enrique.bank.util.JwtTestUtils.service;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.YearMonth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.enrique.bank.commons.enums.AccountStatus;
import dev.enrique.bank.dao.CardRepository;
import dev.enrique.bank.util.AccountFixtures;
import dev.enrique.bank.util.PostgresContainerConfig;

@SpringBootTest
@AutoConfigureMockMvc
class CardControllerTest extends PostgresContainerConfig {
    private static final String ACCOUNT_NUMBER = "ACC-1";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AccountFixtures fixtures;
    @Autowired
    private CardRepository cardRepository;
    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        fixtures.clear();
        fixtures.account("user-1", ACCOUNT_NUMBER, "646180000000000001", AccountStatus.OPEN, "500.00");
    }

    // ---- issue ----

    @Test
    void issue_returnsTheFullNumberAndCvvOnlyOnce() throws Exception {
        JsonNode card = issue(customer("user-1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cardHolderName").value("ANA MARTÍNEZ"))
                .andExpect(jsonPath("$.cardType").value("DEBIT"))
                .andExpect(jsonPath("$.cardStatus").value("ACTIVE"))
                .andReturn().getResponse().getContentAsString().transform(this::json);

        String number = card.get("cardNumber").asText();
        LocalDate expiration = LocalDate.parse(card.get("expirationDate").asText());
        assertThat(number).hasSize(16).startsWith("412345");
        assertThat(luhnValid(number)).isTrue();
        assertThat(expiration).isEqualTo(YearMonth.now().plusYears(5).atEndOfMonth());
        assertThat(card.get("cvv").asText()).matches("\\d{3}");

        // Afterwards only the last four digits are shown, without the cvv
        mockMvc.perform(get(CARD + "/me").with(customer("user-1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].maskedNumber").value("**** **** **** " + number.substring(12)))
                .andExpect(jsonPath("$[0].cardNumber").doesNotExist())
                .andExpect(jsonPath("$[0].cvv").doesNotExist());

        var stored = cardRepository.findAll().get(0);
        assertThat(stored.getCardNumber()).isEqualTo(number);
        assertThat(stored.getCvv()).isEqualTo(card.get("cvv").asText());
    }

    @Test
    void issue_whileTheAccountHasACardInUse_returns409_untilItIsCancelled() throws Exception {
        long id = issue(customer("user-1")).andReturn().getResponse().getContentAsString()
                .transform(this::json).get("id").asLong();

        issue(customer("user-1")).andExpect(status().isConflict());
        action(id, "block", customer("user-1")).andExpect(status().isOk());
        issue(customer("user-1")).andExpect(status().isConflict());

        action(id, "cancel", customer("user-1")).andExpect(status().isOk());
        issue(customer("user-1")).andExpect(status().isCreated());
    }

    @Test
    void issue_forFrozenAccount_returns422() throws Exception {
        fixtures.account("user-2", "ACC-2", "646180000000000002", AccountStatus.FROZEN, "0.00");

        issue(customer("user-2"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Account ACC-2 is FROZEN and cannot operate"));
    }

    @Test
    void issue_withoutAccount_returns404() throws Exception {
        issue(customer("user-9")).andExpect(status().isNotFound());
    }

    @Test
    void issue_withoutNameInToken_returns400() throws Exception {
        issue(jwt().jwt(token -> token.subject("user-1").claim("registration_status", "COMPLETE"))
                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER_BASIC")))
                .andExpect(status().isBadRequest());
        assertThat(cardRepository.count()).isZero();
    }

    @Test
    void issue_asAdminOrService_returns403() throws Exception {
        issue(admin()).andExpect(status().isForbidden());
        issue(service()).andExpect(status().isForbidden());
    }

    // ---- block / unblock / cancel ----

    @Test
    void blockUnblockCancel_followTheAllowedTransitions() throws Exception {
        long id = issuedId();

        action(id, "block", customer("user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardStatus").value("BLOCKED"));
        action(id, "block", customer("user-1")).andExpect(status().isOk());
        action(id, "unblock", customer("user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardStatus").value("ACTIVE"));
        action(id, "cancel", customer("user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardStatus").value("CANCELLED"));

        // A cancelled card is final
        action(id, "unblock", customer("user-1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Card is CANCELLED and cannot change to ACTIVE"));
        action(id, "block", customer("user-1")).andExpect(status().isConflict());
    }

    @Test
    void cardOfAnotherUser_returns404_butAdminCanChangeIt() throws Exception {
        long id = issuedId();

        action(id, "block", customer("user-2")).andExpect(status().isNotFound());
        action(id, "block", admin())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardStatus").value("BLOCKED"));
        action(999_999, "block", admin()).andExpect(status().isNotFound());
    }

    // ---- lookup by card number ----

    @Test
    void getByCardNumber_asService_returnsTheAccount() throws Exception {
        String number = issuedNumber();

        mockMvc.perform(get(ACCOUNT + "/get-by-card-number/{cardNumber}", number).with(service()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value(ACCOUNT_NUMBER))
                .andExpect(jsonPath("$.userId").value("user-1"));
        mockMvc.perform(get(ACCOUNT + "/get-by-card-number/{cardNumber}", "4000000000000002").with(service()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Card not found"));
        mockMvc.perform(get(ACCOUNT + "/get-by-card-number/{cardNumber}", number).with(customer("user-1")))
                .andExpect(status().isForbidden());
    }

    // ---- payment with an issued card ----

    @Test
    void issuedCard_paysWithItsCvv_andNotOnceBlocked() throws Exception {
        JsonNode card = issue(customer("user-1")).andReturn().getResponse().getContentAsString()
                .transform(this::json);
        String body = "{\"currency\":\"MX\",\"accountNumber\":\"" + ACCOUNT_NUMBER + "\",\"amount\":50.00,\"sourceCardNumber\":\""
                + card.get("cardNumber").asText() + "\",\"cvv\":\"" + card.get("cvv").asText() + "\"}";

        purchase("txn-1", body)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionStatus").value("COMPLETED"));
        assertThat(fixtures.balanceOf(ACCOUNT_NUMBER)).isEqualByComparingTo("450.00");

        action(card.get("id").asLong(), "block", customer("user-1")).andExpect(status().isOk());
        purchase("txn-2", body)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Card declined: the card is BLOCKED"));
        assertThat(fixtures.balanceOf(ACCOUNT_NUMBER)).isEqualByComparingTo("450.00");
    }

    private ResultActions issue(RequestPostProcessor auth) throws Exception {
        return mockMvc.perform(post(CARD).with(auth));
    }

    private ResultActions action(long cardId, String action, RequestPostProcessor auth) throws Exception {
        return mockMvc.perform(post(CARD + "/{cardId}/" + action, cardId).with(auth));
    }

    private ResultActions purchase(String key, String body) throws Exception {
        return mockMvc.perform(post(MOVEMENT + "/purchase")
                .with(service())
                .header(IDEMPOTENCY_KEY, key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private long issuedId() throws Exception {
        return issue(customer("user-1")).andReturn().getResponse().getContentAsString()
                .transform(this::json).get("id").asLong();
    }

    private String issuedNumber() throws Exception {
        return issue(customer("user-1")).andReturn().getResponse().getContentAsString()
                .transform(this::json).get("cardNumber").asText();
    }

    private JsonNode json(String content) {
        try {
            return objectMapper.readTree(content);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static boolean luhnValid(String number) {
        int sum = 0;
        for (int i = number.length() - 1, position = 0; i >= 0; i--, position++) {
            int digit = Character.getNumericValue(number.charAt(i));
            if (position % 2 == 1) {
                digit = digit * 2 > 9 ? digit * 2 - 9 : digit * 2;
            }
            sum += digit;
        }
        return sum % 10 == 0;
    }
}
