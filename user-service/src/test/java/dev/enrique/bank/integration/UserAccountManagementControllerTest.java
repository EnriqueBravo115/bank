package dev.enrique.bank.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.enrique.bank.broker.Topics;
import dev.enrique.bank.broker.producer.UserEventProducer;
import dev.enrique.bank.commons.dto.request.UserRegisterRequest;
import dev.enrique.bank.commons.enums.OutboxStatus;
import dev.enrique.bank.commons.enums.RegisterStatus;
import dev.enrique.bank.commons.enums.UserRole;
import dev.enrique.bank.dao.OutboxEventRepository;
import dev.enrique.bank.dao.UserFinancialInfoRepository;
import dev.enrique.bank.dao.UserKycRepository;
import dev.enrique.bank.dao.UserProfileRepository;
import dev.enrique.bank.dao.UserRepository;
import dev.enrique.bank.model.OutboxEvent;
import dev.enrique.bank.model.User;
import dev.enrique.bank.service.KeycloakUserService;
import dev.enrique.bank.util.FullContainerConfig;
import dev.enrique.bank.util.Mailbox;

/** Block/unblock (ADMIN) and contact changes (customer), checked against the real Keycloak. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test-keycloak")
class UserAccountManagementControllerTest extends FullContainerConfig {
    private static final String EMAIL = "manage@test.com";
    private static final String NEW_EMAIL = "manage.new@test.com";
    private static final String PHONE = "5512345678";
    private static final String PASSWORD = "Password123!";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserProfileRepository userProfileRepository;
    @Autowired
    private UserKycRepository userKycRepository;
    @Autowired
    private UserFinancialInfoRepository userFinancialInfoRepository;
    @Autowired
    private OutboxEventRepository outboxEventRepository;
    @Autowired
    private KeycloakUserService keycloakUserService;
    @Autowired
    private Keycloak keycloak;
    @Value("${keycloak.realm}")
    private String realm;
    @Value("${keycloak.auth-server-url}")
    private String authServerUrl;
    @Value("${keycloak.client-id}")
    private String clientId;
    @Value("${keycloak.client-secret}")
    private String clientSecret;

    @MockitoBean
    private UserEventProducer userEventProducer;

    private final Mailbox mailbox = new Mailbox(mailpitUrl());

    @BeforeEach
    void cleanUp() {
        mailbox.clear();
        outboxEventRepository.deleteAll();
        userFinancialInfoRepository.deleteAll();
        userKycRepository.deleteAll();
        userProfileRepository.deleteAll();
        userRepository.deleteAll();
        for (String email : List.of(EMAIL, NEW_EMAIL, "other@test.com", "keycloak.only@test.com")) {
            keycloak.realm(realm).users().searchByEmail(email, true)
                    .forEach(user -> keycloak.realm(realm).users().get(user.getId()).remove());
        }
    }

    // ---- block / unblock ----

    @Test
    void shouldBlockUser() throws Exception {
        User user = createUser(EMAIL, PHONE);

        updateActive(user.getId(), false, admin())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        assertThat(userInDb(user).isActive()).isFalse();
        assertThat(keycloakUser(user).isEnabled()).isFalse();
        assertThat(loginStatus(EMAIL).is4xxClientError()).as("a blocked user cannot log in").isTrue();
        assertThat(singleEvent(Topics.USER_STATUS_CHANGED).getPayload()).contains("false");
    }

    @Test
    void shouldRejectRequestsFromBlockedUserWithStillValidToken() throws Exception {
        User user = createUser(EMAIL, PHONE);
        mockMvc.perform(get("/api/v1/user/me").with(customer(user.getKeycloakId()))).andExpect(status().isOk());

        updateActive(user.getId(), false, admin()).andExpect(status().isOk());

        // Same token as before the block: it has not expired, but the user is blocked.
        mockMvc.perform(get("/api/v1/user/me").with(customer(user.getKeycloakId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("User blocked"));
        updatePhone(user, "+52", "5587654321").andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/user/register/me/verify-email").with(customer(user.getKeycloakId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("User blocked"));

        assertThat(userInDb(user).getPhoneNumber()).isEqualTo(PHONE);
    }

    @Test
    void shouldAcceptRequestsAgainAfterUnblock() throws Exception {
        User user = createUser(EMAIL, PHONE);
        updateActive(user.getId(), false, admin()).andExpect(status().isOk());

        updateActive(user.getId(), true, admin()).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/user/me").with(customer(user.getKeycloakId()))).andExpect(status().isOk());
    }

    @Test
    void shouldUnblockUser() throws Exception {
        User user = createUser(EMAIL, PHONE);
        updateActive(user.getId(), false, admin()).andExpect(status().isOk());

        updateActive(user.getId(), true, admin())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        assertThat(userInDb(user).isActive()).isTrue();
        assertThat(keycloakUser(user).isEnabled()).isTrue();
        assertThat(loginStatus(EMAIL).is2xxSuccessful()).isTrue();
        assertThat(outboxEventRepository.findAll()).hasSize(2);
    }

    @Test
    void shouldNotEmitEventWhenStatusDoesNotChange() throws Exception {
        User user = createUser(EMAIL, PHONE);

        updateActive(user.getId(), true, admin()).andExpect(status().isOk());

        assertThat(outboxEventRepository.count()).isZero();
    }

    @Test
    void shouldRejectBlockWithoutBody() throws Exception {
        User user = createUser(EMAIL, PHONE);

        mockMvc.perform(put("/api/v1/user/{userId}/active", user.getId()).with(admin())
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnNotFoundWhenBlockingUnknownUser() throws Exception {
        updateActive(999_999L, false, admin()).andExpect(status().isNotFound());
    }

    @Test
    void shouldForbidCustomersFromBlocking() throws Exception {
        User user = createUser(EMAIL, PHONE);

        updateActive(user.getId(), false, customer(user.getKeycloakId())).andExpect(status().isForbidden());

        assertThat(userInDb(user).isActive()).isTrue();
    }

    // ---- email ----

    @Test
    void shouldUpdateEmailInDbAndKeycloak() throws Exception {
        User user = createUser(EMAIL, PHONE);

        updateEmail(user, NEW_EMAIL)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(NEW_EMAIL));

        assertThat(userInDb(user).getEmail()).isEqualTo(NEW_EMAIL);
        UserRepresentation inKeycloak = keycloakUser(user);
        assertThat(inKeycloak.getEmail()).isEqualTo(NEW_EMAIL);
        assertThat(inKeycloak.getUsername()).isEqualTo(NEW_EMAIL);
        assertThat(inKeycloak.isEmailVerified()).as("the new email must be verified again").isFalse();
        assertThat(mailbox.countTo(NEW_EMAIL)).isEqualTo(1);
        assertThat(loginStatus(NEW_EMAIL).is2xxSuccessful()).isTrue();
        assertThat(objectMapper.readTree(singleEvent(Topics.USER_EMAIL_CHANGED).getPayload()).get("email").asText())
                .isEqualTo(NEW_EMAIL);
    }

    @Test
    void shouldRejectEmailOfAnotherUser() throws Exception {
        User user = createUser(EMAIL, PHONE);
        createUser("other@test.com", "5500000000");

        updateEmail(user, "OTHER@test.com")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A user with this email is already registered"));

        assertThat(userInDb(user).getEmail()).isEqualTo(EMAIL);
    }

    @Test
    void shouldRollBackWhenEmailOnlyExistsInKeycloak() throws Exception {
        User user = createUser(EMAIL, PHONE);
        keycloakUserService.createUser(registerRequest("keycloak.only@test.com", "5500000001"));

        updateEmail(user, "keycloak.only@test.com").andExpect(status().isConflict());

        assertThat(userInDb(user).getEmail()).as("DB change rolled back").isEqualTo(EMAIL);
        assertThat(keycloakUser(user).getEmail()).isEqualTo(EMAIL);
        assertThat(outboxEventRepository.count()).as("event rolled back").isZero();
    }

    @Test
    void shouldIgnoreSameEmail() throws Exception {
        User user = createUser(EMAIL, PHONE);

        updateEmail(user, EMAIL.toUpperCase()).andExpect(status().isOk());

        assertThat(outboxEventRepository.count()).isZero();
        assertThat(mailbox.countTo(EMAIL)).isZero();
    }

    @Test
    void shouldRejectInvalidEmail() throws Exception {
        User user = createUser(EMAIL, PHONE);

        updateEmail(user, "not-an-email").andExpect(status().isBadRequest());
    }

    @Test
    void shouldNotLetUserChangeEmailThroughKeycloakAccountApi() throws Exception {
        User user = createUser(EMAIL, PHONE);
        String token = accessToken(EMAIL);

        // Control: the same call works for an attribute the user may edit.
        assertThat(updateOwnAccount(token, EMAIL, EMAIL, "Renamed").is2xxSuccessful()).isTrue();
        assertThat(keycloakUser(user).getFirstName()).isEqualTo("Renamed");

        assertThat(updateOwnAccount(token, NEW_EMAIL, NEW_EMAIL, "Renamed").is4xxClientError()).isTrue();
        assertThat(keycloakUser(user).getEmail()).isEqualTo(EMAIL);
        assertThat(keycloakUser(user).getUsername()).isEqualTo(EMAIL);
    }

    // ---- phone ----

    @Test
    void shouldUpdatePhone() throws Exception {
        User user = createUser(EMAIL, PHONE);

        updatePhone(user, "+1", "5599999999").andExpect(status().isOk());

        User updated = userInDb(user);
        assertThat(updated.getPhoneCode()).isEqualTo("+1");
        assertThat(updated.getPhoneNumber()).isEqualTo("5599999999");
    }

    @Test
    void shouldAllowKeepingOwnPhone() throws Exception {
        User user = createUser(EMAIL, PHONE);

        updatePhone(user, "+52", PHONE).andExpect(status().isOk());
    }

    @Test
    void shouldRejectPhoneOfAnotherUser() throws Exception {
        User user = createUser(EMAIL, PHONE);
        createUser("other@test.com", "5500000000");

        updatePhone(user, "+52", "5500000000")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A user with this phone number is already registered"));

        assertThat(userInDb(user).getPhoneNumber()).isEqualTo(PHONE);
    }

    @Test
    void shouldRejectInvalidPhone() throws Exception {
        User user = createUser(EMAIL, PHONE);

        updatePhone(user, "+52", "12345").andExpect(status().isBadRequest());
        updatePhone(user, "+44", PHONE).andExpect(status().isBadRequest());
    }

    // ---- helpers ----

    private ResultActions updateActive(Long userId, boolean active, RequestPostProcessor auth) throws Exception {
        return mockMvc.perform(put("/api/v1/user/{userId}/active", userId).with(auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\": " + active + "}"));
    }

    private ResultActions updateEmail(User user, String email) throws Exception {
        return mockMvc.perform(put("/api/v1/user/me/email").with(customer(user.getKeycloakId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"" + email + "\"}"));
    }

    private ResultActions updatePhone(User user, String phoneCode, String phoneNumber) throws Exception {
        return mockMvc.perform(put("/api/v1/user/me/phone").with(customer(user.getKeycloakId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phoneCode\": \"" + phoneCode + "\", \"phoneNumber\": \"" + phoneNumber + "\"}"));
    }

    // A user that finished registration: Keycloak user plus DB row.
    private User createUser(String email, String phone) {
        String keycloakId = keycloakUserService.createUser(registerRequest(email, phone));
        keycloakUserService.assignRole(keycloakId, UserRole.CUSTOMER_BASIC);

        User user = new User();
        user.setKeycloakId(keycloakId);
        user.setEmail(email);
        user.setPhoneCode("+52");
        user.setPhoneNumber(phone);
        user.setActive(true);
        user.setRole(UserRole.CUSTOMER_BASIC);
        user.setRegisterStatus(RegisterStatus.COMPLETE);
        return userRepository.save(user);
    }

    private static UserRegisterRequest registerRequest(String email, String phone) {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setEmail(email);
        request.setPassword(PASSWORD);
        request.setPhoneCode("+52");
        request.setPhoneNumber(phone);
        return request;
    }

    private HttpStatusCode loginStatus(String username) {
        return RestClient.create()
                .post()
                .uri(authServerUrl + "/realms/" + realm + "/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("grant_type=password&client_id=" + clientId + "&client_secret=" + clientSecret
                        + "&username=" + username + "&password=" + PASSWORD)
                .exchange((request, response) -> response.getStatusCode());
    }

    // Keycloak's account REST API, the one behind the account console at /realms/bank/account.
    private HttpStatusCode updateOwnAccount(String token, String username, String email, String firstName) {
        String body = """
                {"username": "%s", "email": "%s", "firstName": "%s", "lastName": "PENDING"}""".formatted(username, email, firstName);
        return RestClient.create()
                .post()
                .uri(authServerUrl + "/realms/" + realm + "/account/")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .exchange((request, response) -> response.getStatusCode());
    }

    @SuppressWarnings("unchecked")
    private String accessToken(String username) {
        java.util.Map<String, Object> response = RestClient.create()
                .post()
                .uri(authServerUrl + "/realms/" + realm + "/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("grant_type=password&client_id=" + clientId + "&client_secret=" + clientSecret
                        + "&username=" + username + "&password=" + PASSWORD)
                .retrieve()
                .body(java.util.Map.class);
        return (String) response.get("access_token");
    }

    private User userInDb(User user) {
        return userRepository.findById(user.getId()).orElseThrow();
    }

    private UserRepresentation keycloakUser(User user) {
        return keycloak.realm(realm).users().get(user.getKeycloakId()).toRepresentation();
    }

    private OutboxEvent singleEvent(String topic) {
        List<OutboxEvent> events = outboxEventRepository.findAll();
        assertThat(events).singleElement().satisfies(event -> {
            assertThat(event.getTopic()).isEqualTo(topic);
            assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        });
        return events.get(0);
    }

    private static RequestPostProcessor admin() {
        return jwt().jwt(j -> j.subject("admin-keycloak-id").claim("registration_status", "COMPLETE"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private static RequestPostProcessor customer(String keycloakId) {
        return jwt().jwt(j -> j.subject(keycloakId).claim("registration_status", "COMPLETE"))
                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER_BASIC"));
    }
}
