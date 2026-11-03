package dev.enrique.bank.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.enrique.bank.broker.producer.AccountProducer;
import dev.enrique.bank.commons.dto.request.UserFinancialInfoRequest;
import dev.enrique.bank.commons.dto.request.UserKycDataRequest;
import dev.enrique.bank.commons.dto.request.UserProfileRequest;
import dev.enrique.bank.commons.dto.request.UserRegisterRequest;
import dev.enrique.bank.commons.enums.Country;
import dev.enrique.bank.commons.enums.DocumentType;
import dev.enrique.bank.commons.enums.Gender;
import dev.enrique.bank.commons.enums.IncomeSource;
import dev.enrique.bank.commons.enums.MaritalStatus;
import dev.enrique.bank.commons.enums.OccupationType;
import dev.enrique.bank.commons.enums.RegisterStatus;
import dev.enrique.bank.dao.UserFinancialInfoRepository;
import dev.enrique.bank.dao.UserKycRepository;
import dev.enrique.bank.dao.UserProfileRepository;
import dev.enrique.bank.dao.UserRepository;
import dev.enrique.bank.util.FullContainerConfig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
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
    @Autowired
    private UserRepository userRepository;
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
    private AccountProducer accountProducer;

    @BeforeEach
    void cleanUp() {
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
                .andExpect(jsonPath("$.role").value("CUSTOMER_BASIC"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.registerStatus").value("REGISTER"));
    }

    @Test
    void shouldUpdateProfile() throws Exception {
        String token = registerAndLogin(EMAIL, PHONE);

        stepUpdateProfile(token);

        assertThat(registerStatusOf(EMAIL)).isEqualTo(RegisterStatus.PROFILE);
    }

    @Test
    void shouldUpdateKyc() throws Exception {
        String token = registerAndLogin(EMAIL, PHONE);
        stepUpdateProfile(token);

        stepUpdateKyc(token);

        assertThat(registerStatusOf(EMAIL)).isEqualTo(RegisterStatus.KYC);
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
    void shouldRollbackKeycloakUserWhenDatabaseRejectsRegistration() throws Exception {
        stepRegister(EMAIL, PHONE);

        mockMvc.perform(post("/api/v1/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildRegisterRequest(OTHER_EMAIL, PHONE))))
                .andExpect(status().isConflict());

        assertThat(keycloak.realm(realm).users().searchByEmail(OTHER_EMAIL, true)).isEmpty();
        assertThat(userRepository.count()).isEqualTo(1);
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

    private void stepRegister(String email, String phone) throws Exception {
        mockMvc.perform(post("/api/v1/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildRegisterRequest(email, phone))))
                .andExpect(status().isOk());
    }

    private String registerAndLogin(String email, String phone) throws Exception {
        stepRegister(email, phone);
        return login(email);
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

    private RegisterStatus registerStatusOf(String email) {
        return userRepository.findAll().stream()
                .filter(user -> user.getEmail().equals(email))
                .findFirst()
                .orElseThrow()
                .getRegisterStatus();
    }

    private void stepUpdateProfile(String token) throws Exception {
        mockMvc.perform(put("/api/v1/user/register/me/profile")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildProfileRequest())))
                .andExpect(status().isNoContent());
    }

    private void stepUpdateKyc(String token) throws Exception {
        mockMvc.perform(put("/api/v1/user/register/me/kyc")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildKycRequest())))
                .andExpect(status().isNoContent());
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
        request.setBirthday("01/01/1990");
        request.setCountryOfBirth(Country.MX);
        return request;
    }

    private UserKycDataRequest buildKycRequest() {
        UserKycDataRequest request = new UserKycDataRequest();
        request.setCurp("DOEJ900101HDFLNS09");
        request.setRfc("DOEJ900101ABC");
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
