package dev.enrique.bank.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.enrique.bank.broker.Topics;
import dev.enrique.bank.broker.producer.UserEventProducer;
import dev.enrique.bank.commons.dto.request.UserFinancialInfoRequest;
import dev.enrique.bank.commons.dto.request.UserKycDataRequest;
import dev.enrique.bank.commons.dto.request.UserProfileRequest;
import dev.enrique.bank.commons.dto.request.UserRegisterRequest;
import dev.enrique.bank.commons.enums.AccountType;
import dev.enrique.bank.commons.enums.Country;
import dev.enrique.bank.commons.enums.Currency;
import dev.enrique.bank.commons.enums.DocumentType;
import dev.enrique.bank.commons.enums.Gender;
import dev.enrique.bank.commons.enums.IncomeSource;
import dev.enrique.bank.commons.enums.MaritalStatus;
import dev.enrique.bank.commons.enums.OccupationType;
import dev.enrique.bank.commons.enums.OutboxStatus;
import dev.enrique.bank.commons.enums.RegisterStatus;
import dev.enrique.bank.commons.enums.UserRole;
import dev.enrique.bank.dao.OutboxEventRepository;
import dev.enrique.bank.dao.UserFinancialInfoRepository;
import dev.enrique.bank.dao.UserKycRepository;
import dev.enrique.bank.dao.UserProfileRepository;
import dev.enrique.bank.dao.UserRepository;
import dev.enrique.bank.model.User;
import dev.enrique.bank.service.KeycloakUserService;
import dev.enrique.bank.util.FullContainerConfig;
import dev.enrique.bank.util.Mailbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test-keycloak")
public class RegisterControllerTest extends FullContainerConfig {
    private static final String EMAIL = "test@test.com";
    private static final String OTHER_EMAIL = "other@test.com";
    private static final String PHONE = "1234567890";
    private static final String OTHER_PHONE = "0987654321";
    private static final String PASSWORD = "Password123!";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoSpyBean
    private UserRepository userRepository;
    @MockitoSpyBean
    private KeycloakUserService keycloakUserService;
    @Autowired
    private OutboxEventRepository outboxEventRepository;
    @Autowired
    private UserProfileRepository userProfileRepository;
    @Autowired
    private UserKycRepository userKycRepository;
    @Autowired
    private UserFinancialInfoRepository userFinancialInfoRepository;
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

        keycloak.realm(realm)
                .users()
                .list()
                .forEach(user -> keycloak.realm(realm)
                        .users()
                        .get(user.getId())
                        .remove());
    }

    @Test
    void shouldRegisterUser() throws Exception {
        mockMvc.perform(post("/api/v1/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildRegisterRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.keycloakId").isNotEmpty())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.role").doesNotExist())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.registerStatus").value("REGISTER"));
    }

    @Test
    void shouldUpdateProfile() throws Exception {
        String token = registerAndLogin(EMAIL, PHONE);

        stepUpdateProfile(token);

        assertThat(registerStatusOf(EMAIL)).isEqualTo(RegisterStatus.PROFILE);
        assertThat(userOf(EMAIL).getRole()).isNull();
    }

    @Test
    void shouldUpdateKyc() throws Exception {
        String token = registerAndLogin(EMAIL, PHONE);
        stepUpdateProfile(token);

        stepUpdateKyc(token);

        User user = userOf(EMAIL);
        assertThat(user.getRegisterStatus()).isEqualTo(RegisterStatus.KYC);

        // The account request is stored in the outbox, not sent directly to Kafka.
        assertThat(outboxEventRepository.findAll()).singleElement().satisfies(event -> {
            assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
            assertThat(event.getTopic()).isEqualTo(Topics.ACCOUNT_REQUESTED);
            assertThat(event.getAggregateId()).isEqualTo(user.getKeycloakId());
            JsonNode payload = objectMapper.readTree(event.getPayload());
            assertThat(payload.get("email").asText()).isEqualTo(EMAIL);
            // Nothing chosen: a savings account in pesos
            assertThat(payload.get("accountType").asText()).isEqualTo("SAVING");
            assertThat(payload.get("currency").asText()).isEqualTo("MX");
        });
        verifyNoInteractions(userEventProducer);
        assertThat(user.getRole()).isEqualTo(UserRole.CUSTOMER_BASIC);
        assertThat(keycloak.realm(realm).users().get(user.getKeycloakId()).roles().realmLevel().listAll())
                .extracting(RoleRepresentation::getName)
                .contains(UserRole.CUSTOMER_BASIC.name());
    }

    @Test
    void shouldUndoKeycloakRoleWhenKycFailsHalfway() throws Exception {
        String token = registerAndLogin(EMAIL, PHONE);
        stepUpdateProfile(token);
        User user = userOf(EMAIL);
        // assignRole already went through when this fails.
        doThrow(new IllegalStateException("Keycloak down"))
                .when(keycloakUserService).updateRegistrationStatus(user.getKeycloakId(), RegisterStatus.KYC.name());

        assertThatThrownBy(() -> kycRequest(token, buildKycRequest())).hasRootCauseMessage("Keycloak down");

        assertThat(registerStatusOf(EMAIL)).isEqualTo(RegisterStatus.PROFILE);
        assertThat(userOf(EMAIL).getRole()).isNull();
        assertThat(outboxEventRepository.count()).as("account request rolled back").isZero();
        assertThat(keycloak.realm(realm).users().get(user.getKeycloakId()).roles().realmLevel().listAll())
                .extracting(RoleRepresentation::getName)
                .doesNotContain(UserRole.CUSTOMER_BASIC.name());
        assertThat(keycloakUser(EMAIL).getAttributes().get("registration_status"))
                .containsExactly(RegisterStatus.PROFILE.name());

        // Keycloak is back: the step can be retried.
        reset(keycloakUserService);
        stepUpdateKyc(token);
        assertThat(registerStatusOf(EMAIL)).isEqualTo(RegisterStatus.KYC);
    }

    @Test
    void shouldOpenTheAccountTypeAndCurrencyChosenAtKyc() throws Exception {
        String token = registerAndLogin(EMAIL, PHONE);
        stepUpdateProfile(token);
        UserKycDataRequest request = buildKycRequest();
        request.setAccountType(AccountType.CHECKING);
        request.setCurrency(Currency.US);

        kycRequest(token, request).andExpect(status().isNoContent());

        assertThat(outboxEventRepository.findAll()).singleElement().satisfies(event -> {
            JsonNode payload = objectMapper.readTree(event.getPayload());
            assertThat(payload.get("accountType").asText()).isEqualTo("CHECKING");
            assertThat(payload.get("currency").asText()).isEqualTo("US");
        });
    }

    @Test
    void shouldRejectKycWithoutDocumentType() throws Exception {
        String token = registerAndLogin(EMAIL, PHONE);
        stepUpdateProfile(token);
        UserKycDataRequest request = buildKycRequest();
        request.setDocumentType(null);

        kycRequest(token, request).andExpect(status().isBadRequest());

        assertThat(registerStatusOf(EMAIL)).isEqualTo(RegisterStatus.PROFILE);
    }

    @Test
    void shouldUndoKeycloakNameAndStepWhenProfileFailsAfterUpdatingKeycloak() throws Exception {
        String token = registerAndLogin(EMAIL, PHONE);
        String keycloakId = userOf(EMAIL).getKeycloakId();
        // Keycloak is updated and then the transaction fails (as a failed commit would)
        failAfterCalling(keycloakId, RegisterStatus.PROFILE);

        assertThatThrownBy(() -> stepUpdateProfile(token)).hasRootCauseMessage("commit failed");

        assertThat(registerStatusOf(EMAIL)).isEqualTo(RegisterStatus.REGISTER);
        UserRepresentation keycloakUser = keycloakUser(EMAIL);
        assertThat(keycloakUser.getFirstName()).isEqualTo("PENDING");
        assertThat(keycloakUser.getLastName()).isEqualTo("PENDING");
        assertThat(keycloakUser.getAttributes().get("registration_status"))
                .containsExactly(RegisterStatus.REGISTER.name());
    }

    @Test
    void shouldUndoKeycloakStepWhenFinancialInfoFailsAfterUpdatingKeycloak() throws Exception {
        String token = registerAndLogin(EMAIL, PHONE);
        stepUpdateProfile(token);
        stepUpdateKyc(token);
        failAfterCalling(userOf(EMAIL).getKeycloakId(), RegisterStatus.COMPLETE);

        assertThatThrownBy(() -> stepUpdateFinancial(token)).hasRootCauseMessage("commit failed");

        assertThat(registerStatusOf(EMAIL)).isEqualTo(RegisterStatus.KYC);
        assertThat(keycloakUser(EMAIL).getAttributes().get("registration_status"))
                .containsExactly(RegisterStatus.KYC.name());
    }

    @Test
    void shouldRegisterProfileWithoutSecondSurname() throws Exception {
        String token = registerAndLogin(EMAIL, PHONE);

        profileRequest(token, """
                {"names":"John","firstSurname":"Doe","gender":"MALE",
                 "birthday":"01/01/1990","countryOfBirth":"US"}""")
                .andExpect(status().isNoContent());

        assertThat(userProfileRepository.findAll()).singleElement()
                .satisfies(profile -> assertThat(profile.getSecondSurname()).isNull());
        assertThat(keycloakUser(EMAIL).getLastName()).isEqualTo("Doe");
    }

    @Test
    void shouldUpdateFinancialInfo() throws Exception {
        String token = registerAndLogin(EMAIL, PHONE);
        stepUpdateProfile(token);
        stepUpdateKyc(token);

        stepUpdateFinancial(token);

        assertThat(registerStatusOf(EMAIL)).isEqualTo(RegisterStatus.COMPLETE);
    }

    @Test
    void shouldRejectRegistrationStepsWithoutToken() throws Exception {
        stepRegister(EMAIL, PHONE);

        mockMvc.perform(put("/api/v1/user/register/me/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildProfileRequest())))
                .andExpect(status().isUnauthorized());

        assertThat(registerStatusOf(EMAIL)).isEqualTo(RegisterStatus.REGISTER);
    }

    @Test
    void shouldOnlyUpdateRegistrationOfTokenOwner() throws Exception {
        stepRegister(EMAIL, PHONE);
        String otherToken = registerAndLogin(OTHER_EMAIL, OTHER_PHONE);

        stepUpdateProfile(otherToken);

        assertThat(registerStatusOf(EMAIL)).isEqualTo(RegisterStatus.REGISTER);
        assertThat(registerStatusOf(OTHER_EMAIL)).isEqualTo(RegisterStatus.PROFILE);
    }

    @Test
    void shouldKeepBlockingIncompleteUsersOutsideRegistration() throws Exception {
        String token = registerAndLogin(EMAIL, PHONE);

        mockMvc.perform(get("/api/v1/user/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Registration incomplete"));
    }

    @Test
    void shouldRejectDuplicateEmailWithoutTouchingKeycloak() throws Exception {
        stepRegister(EMAIL, PHONE);

        registerRequest(EMAIL.toUpperCase(), OTHER_PHONE)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A user with this email is already registered"));

        assertThat(keycloak.realm(realm).users().searchByEmail(EMAIL, true)).hasSize(1);
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldRejectDuplicatePhoneWithoutTouchingKeycloak() throws Exception {
        stepRegister(EMAIL, PHONE);

        registerRequest(OTHER_EMAIL, PHONE)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A user with this phone number is already registered"));

        assertThat(keycloak.realm(realm).users().searchByEmail(OTHER_EMAIL, true)).isEmpty();
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldAcceptTheSameNumberWithAnotherCountryCode() throws Exception {
        stepRegister(EMAIL, PHONE);
        UserRegisterRequest request = buildRegisterRequest(OTHER_EMAIL, PHONE);
        request.setPhoneCode("+1");

        mockMvc.perform(post("/api/v1/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        assertThat(userRepository.count()).isEqualTo(2);
    }

    @Test
    void shouldStoreTheEmailInLowercase() throws Exception {
        registerRequest("Mixed.Case@Test.COM", PHONE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("mixed.case@test.com"));

        assertThat(userOf("mixed.case@test.com").getEmail()).isEqualTo("mixed.case@test.com");
    }

    @Test
    void shouldRejectEmailThatOnlyExistsInKeycloak() throws Exception {
        // e.g. an admin created from the Keycloak console, with no row in user_db
        keycloakUserService.createUser(buildRegisterRequest(EMAIL, PHONE));

        registerRequest(EMAIL, PHONE)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A user with this email is already registered"));

        assertThat(userRepository.count()).isZero();
    }

    @Test
    void shouldRollbackKeycloakUserWhenDatabaseRejectsRegistration() throws Exception {
        // Simulates a concurrent duplicate that passes the pre-checks and fails on the unique constraint.
        doThrow(new DataIntegrityViolationException("users_phone_code_number_key"))
                .when(userRepository).saveAndFlush(any());

        registerRequest(EMAIL, PHONE).andExpect(status().isConflict());

        assertThat(keycloak.realm(realm).users().searchByEmail(EMAIL, true)).isEmpty();
        assertThat(userRepository.count()).isZero();
    }

    @Test
    void shouldRejectDuplicateCurp() throws Exception {
        String firstToken = registerAndLogin(EMAIL, PHONE);
        stepUpdateProfile(firstToken);
        stepUpdateKyc(firstToken);

        String secondToken = registerAndLogin(OTHER_EMAIL, OTHER_PHONE);
        stepUpdateProfile(secondToken);
        UserKycDataRequest sameCurp = buildKycRequest();
        sameCurp.setRfc("DOEJ900101XY2");

        kycRequest(secondToken, sameCurp)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A user with this CURP is already registered"));

        assertThat(registerStatusOf(OTHER_EMAIL)).isEqualTo(RegisterStatus.PROFILE);
        assertThat(outboxEventRepository.count()).as("only the first KYC requests an account").isEqualTo(1);
    }

    @Test
    void shouldRejectDuplicateRfc() throws Exception {
        String firstToken = registerAndLogin(EMAIL, PHONE);
        stepUpdateProfile(firstToken);
        stepUpdateKyc(firstToken);

        String secondToken = registerAndLogin(OTHER_EMAIL, OTHER_PHONE);
        stepUpdateProfile(secondToken);
        UserKycDataRequest sameRfc = buildKycRequest();
        sameRfc.setCurp("DOEJ900101HDFLNS01");

        kycRequest(secondToken, sameRfc)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A user with this RFC is already registered"));

        assertThat(registerStatusOf(OTHER_EMAIL)).isEqualTo(RegisterStatus.PROFILE);
    }

    @Test
    void shouldRejectProfileStepOutOfOrder() throws Exception {
        String token = registerAndLogin(EMAIL, PHONE);
        stepUpdateProfile(token);

        mockMvc.perform(put("/api/v1/user/register/me/profile")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildProfileRequest())))
                .andExpect(status().isConflict());
    }

    // ---- email verification ----

    @Test
    void shouldSendVerificationEmailOnRegister() throws Exception {
        stepRegister(EMAIL, PHONE);

        assertThat(mailbox.countTo(EMAIL)).isEqualTo(1);
        assertThat(keycloakUser(EMAIL).isEmailVerified()).isFalse();
    }

    @Test
    void shouldNotSendVerificationEmailWhenRegistrationRollsBack() throws Exception {
        doAnswer(invocation -> {
            invocation.callRealMethod();
            throw new IllegalStateException("commit failed");
        }).when(keycloakUserService).updateRegistrationStatus(any(), eq(RegisterStatus.REGISTER.name()));

        assertThatThrownBy(() -> registerRequest(EMAIL, PHONE)).hasRootCauseMessage("commit failed");

        assertThat(keycloak.realm(realm).users().searchByEmail(EMAIL, true)).isEmpty();
        assertThat(userRepository.count()).isZero();
        assertThat(mailbox.countTo(EMAIL)).isZero();
    }

    @Test
    void shouldRejectKycWhenEmailIsNotVerified() throws Exception {
        stepRegister(EMAIL, PHONE);
        String token = login(EMAIL);
        stepUpdateProfile(token);

        kycRequest(token, buildKycRequest())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email must be verified before this step"));

        assertThat(registerStatusOf(EMAIL)).isEqualTo(RegisterStatus.PROFILE);
        assertThat(outboxEventRepository.count()).isZero();
    }

    @Test
    void shouldResendVerificationEmail() throws Exception {
        stepRegister(EMAIL, PHONE);
        String token = login(EMAIL);

        mockMvc.perform(post("/api/v1/user/register/me/verify-email").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());

        assertThat(mailbox.countTo(EMAIL)).isEqualTo(2);
    }

    @Test
    void shouldNotResendVerificationEmailWhenAlreadyVerified() throws Exception {
        String token = registerAndLogin(EMAIL, PHONE);

        mockMvc.perform(post("/api/v1/user/register/me/verify-email").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());

        assertThat(mailbox.countTo(EMAIL)).as("only the one sent at registration").isEqualTo(1);
    }

    // ---- email correction before KYC ----

    @Test
    void shouldFixMistypedEmailBeforeKyc() throws Exception {
        stepRegister(EMAIL, PHONE);
        String token = login(EMAIL);

        emailRequest(token, OTHER_EMAIL)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(OTHER_EMAIL));

        assertThat(userOf(OTHER_EMAIL).getRegisterStatus()).isEqualTo(RegisterStatus.REGISTER);
        UserRepresentation keycloakUser = keycloakUser(OTHER_EMAIL);
        assertThat(keycloakUser.getUsername()).isEqualTo(OTHER_EMAIL);
        assertThat(keycloakUser.isEmailVerified()).isFalse();
        assertThat(mailbox.countTo(OTHER_EMAIL)).isEqualTo(1);
        assertThat(outboxEventRepository.count()).as("no account exists yet").isZero();

        // The user verifies the new address, logs in with it and finishes KYC: the account gets the new email.
        markEmailVerified(OTHER_EMAIL);
        String newToken = login(OTHER_EMAIL);
        stepUpdateProfile(newToken);
        stepUpdateKyc(newToken);
        assertThat(outboxEventRepository.findAll()).singleElement().satisfies(event -> {
            assertThat(event.getTopic()).isEqualTo(Topics.ACCOUNT_REQUESTED);
            assertThat(objectMapper.readTree(event.getPayload()).get("email").asText()).isEqualTo(OTHER_EMAIL);
        });
    }

    @Test
    void shouldRejectFixingEmailToOneAlreadyRegistered() throws Exception {
        stepRegister(OTHER_EMAIL, OTHER_PHONE);
        stepRegister(EMAIL, PHONE);
        String token = login(EMAIL);

        emailRequest(token, OTHER_EMAIL)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A user with this email is already registered"));

        assertThat(userOf(EMAIL).getEmail()).isEqualTo(EMAIL);
        assertThat(keycloakUser(EMAIL).getUsername()).isEqualTo(EMAIL);
    }

    @Test
    void shouldRejectInvalidEmailFix() throws Exception {
        stepRegister(EMAIL, PHONE);

        emailRequest(login(EMAIL), "not-an-email").andExpect(status().isBadRequest());
    }

    // ---- format validations ----

    @Test
    void shouldRejectInvalidCurp() throws Exception {
        String token = registerAndLogin(EMAIL, PHONE);
        stepUpdateProfile(token);
        UserKycDataRequest request = buildKycRequest();
        request.setCurp("DOEJ901301HDFLNS09"); // month 13

        kycRequest(token, request).andExpect(status().isBadRequest());

        request.setCurp("doej900101hdflns09"); // lowercase
        kycRequest(token, request).andExpect(status().isBadRequest());
        assertThat(registerStatusOf(EMAIL)).isEqualTo(RegisterStatus.PROFILE);
    }

    @Test
    void shouldRejectKycThatDoesNotMatchBirthday() throws Exception {
        String token = registerAndLogin(EMAIL, PHONE);
        stepUpdateProfile(token); // born 01/01/1990
        UserKycDataRequest request = buildKycRequest();
        request.setCurp("DOEJ900102HDFLNS09");

        kycRequest(token, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("CURP birth date does not match the birthday of the profile"));

        request = buildKycRequest();
        request.setRfc("DOEJ910101AB1");
        kycRequest(token, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("RFC birth date does not match the birthday of the profile"));

        assertThat(registerStatusOf(EMAIL)).isEqualTo(RegisterStatus.PROFILE);
        assertThat(outboxEventRepository.count()).isZero();
    }

    @Test
    void shouldRejectCurpAndRfcOfDifferentPeople() throws Exception {
        String token = registerAndLogin(EMAIL, PHONE);
        stepUpdateProfile(token);
        UserKycDataRequest request = buildKycRequest();
        request.setRfc("OARO900101AB1");

        kycRequest(token, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("CURP and RFC do not belong to the same person"));

        assertThat(registerStatusOf(EMAIL)).isEqualTo(RegisterStatus.PROFILE);
    }

    @Test
    void shouldRejectInvalidRfc() throws Exception {
        String token = registerAndLogin(EMAIL, PHONE);
        stepUpdateProfile(token);
        UserKycDataRequest request = buildKycRequest();
        request.setRfc("DOEJ900101ABC"); // check digit must be 0-9 or A

        kycRequest(token, request).andExpect(status().isBadRequest());
        assertThat(registerStatusOf(EMAIL)).isEqualTo(RegisterStatus.PROFILE);
    }

    @Test
    void shouldRejectUnderageBirthday() throws Exception {
        String token = registerAndLogin(EMAIL, PHONE);
        String seventeen = LocalDate.now().minusYears(17).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        profileRequest(token, profileJson(seventeen)).andExpect(status().isBadRequest());

        assertThat(registerStatusOf(EMAIL)).isEqualTo(RegisterStatus.REGISTER);
    }

    @Test
    void shouldRejectImpossibleOrMalformedBirthday() throws Exception {
        String token = registerAndLogin(EMAIL, PHONE);

        profileRequest(token, profileJson("31/02/1990")).andExpect(status().isBadRequest());
        profileRequest(token, profileJson("1990-01-01")).andExpect(status().isBadRequest());
        profileRequest(token, """
                {"names":"John","firstSurname":"Doe","secondSurname":"Smith","gender":"MALE","countryOfBirth":"MX"}""")
                .andExpect(status().isBadRequest());

        assertThat(registerStatusOf(EMAIL)).isEqualTo(RegisterStatus.REGISTER);
    }

    @Test
    void shouldAcceptAdultBirthday() throws Exception {
        String token = registerAndLogin(EMAIL, PHONE);

        profileRequest(token, profileJson("29/02/2000")).andExpect(status().isNoContent());

        assertThat(userProfileRepository.findAll()).singleElement()
                .satisfies(profile -> assertThat(profile.getBirthday()).isEqualTo(LocalDate.of(2000, 2, 29)));
    }

    // The real Keycloak call goes through and then the step fails, leaving Keycloak ahead of the database.
    private void failAfterCalling(String keycloakId, RegisterStatus status) {
        doAnswer(invocation -> {
            invocation.callRealMethod();
            throw new IllegalStateException("commit failed");
        }).when(keycloakUserService).updateRegistrationStatus(keycloakId, status.name());
    }

    private ResultActions registerRequest(String email, String phone) throws Exception {
        return mockMvc.perform(post("/api/v1/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildRegisterRequest(email, phone))));
    }

    private void stepRegister(String email, String phone) throws Exception {
        registerRequest(email, phone).andExpect(status().isOk());
    }

    private ResultActions emailRequest(String token, String email) throws Exception {
        return mockMvc.perform(put("/api/v1/user/me/email")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"" + email + "\"}"));
    }

    private ResultActions kycRequest(String token, UserKycDataRequest request) throws Exception {
        return mockMvc.perform(put("/api/v1/user/register/me/kyc")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    // Registers, confirms the email (as if the user clicked the link) and logs in.
    private String registerAndLogin(String email, String phone) throws Exception {
        stepRegister(email, phone);
        markEmailVerified(email);
        return login(email);
    }

    private void markEmailVerified(String email) {
        UserRepresentation user = keycloakUser(email);
        user.setEmailVerified(true);
        keycloak.realm(realm).users().get(user.getId()).update(user);
    }

    private UserRepresentation keycloakUser(String email) {
        return keycloak.realm(realm).users().searchByEmail(email, true).get(0);
    }

    private ResultActions profileRequest(String token, String json) throws Exception {
        return mockMvc.perform(put("/api/v1/user/register/me/profile")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private String profileJson(String birthday) {
        return """
                {"names":"John","firstSurname":"Doe","secondSurname":"Smith","gender":"MALE",
                 "birthday":"%s","countryOfBirth":"MX"}""".formatted(birthday);
    }

    // Same password grant the client uses after POST /register.
    private String login(String email) {
        Map<?, ?> response = RestClient.create()
                .post()
                .uri(authServerUrl + "/realms/" + realm + "/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("grant_type=password&client_id=" + clientId
                        + "&client_secret=" + clientSecret
                        + "&username=" + URLEncoder.encode(email, StandardCharsets.UTF_8)
                        + "&password=" + URLEncoder.encode(PASSWORD, StandardCharsets.UTF_8))
                .retrieve()
                .body(Map.class);
        return (String) response.get("access_token");
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private User userOf(String email) {
        return userRepository.findAll().stream()
                .filter(user -> user.getEmail().equals(email))
                .findFirst()
                .orElseThrow();
    }

    private RegisterStatus registerStatusOf(String email) {
        return userOf(email).getRegisterStatus();
    }

    private void stepUpdateProfile(String token) throws Exception {
        mockMvc.perform(put("/api/v1/user/register/me/profile")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildProfileRequest())))
                .andExpect(status().isNoContent());
    }

    private void stepUpdateKyc(String token) throws Exception {
        kycRequest(token, buildKycRequest()).andExpect(status().isNoContent());
    }

    private void stepUpdateFinancial(String token) throws Exception {
        mockMvc.perform(put("/api/v1/user/register/me/financial")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildFinancialRequest())))
                .andExpect(status().isNoContent());
    }

    private UserRegisterRequest buildRegisterRequest() {
        return buildRegisterRequest(EMAIL, PHONE);
    }

    private UserRegisterRequest buildRegisterRequest(String email, String phone) {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setEmail(email);
        request.setPhoneCode("+52");
        request.setPhoneNumber(phone);
        request.setPassword(PASSWORD);
        return request;
    }

    private UserProfileRequest buildProfileRequest() {
        UserProfileRequest request = new UserProfileRequest();
        request.setNames("John");
        request.setFirstSurname("Doe");
        request.setSecondSurname("Smith");
        request.setGender(Gender.MALE);
        request.setBirthday(LocalDate.of(1990, 1, 1));
        request.setCountryOfBirth(Country.MX);
        return request;
    }

    private UserKycDataRequest buildKycRequest() {
        UserKycDataRequest request = new UserKycDataRequest();
        request.setCurp("DOEJ900101HDFLNS09");
        request.setRfc("DOEJ900101AB1");
        request.setDocumentType(DocumentType.DRIVERS_LICENSE);
        return request;
    }

    private UserFinancialInfoRequest buildFinancialRequest() {
        UserFinancialInfoRequest request = new UserFinancialInfoRequest();
        request.setOccupationType(OccupationType.EMPLOYEE);
        request.setEmployerName("Acme Corp");
        request.setIncomeSource(IncomeSource.SALARY);
        request.setMonthlyIncome(new BigDecimal("15000.00"));
        request.setMaritalStatus(MaritalStatus.SINGLE);
        return request;
    }
}
