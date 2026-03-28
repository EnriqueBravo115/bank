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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test-keycloak")
public class RegisterControllerTest extends FullContainerConfig {
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
                .andExpect(jsonPath("$.email").value("test@test.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER_BASIC"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.registerStatus").value("REGISTER"));
    }

    @Test
    void shouldUpdateProfile() throws Exception {
        Long userId = stepRegister();

        mockMvc.perform(put("/api/v1/user/register/{userId}/profile", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildProfileRequest())))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldUpdateKyc() throws Exception {
        Long userId = stepRegister();
        stepUpdateProfile(userId);

        mockMvc.perform(put("/api/v1/user/register/{userId}/kyc", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildKycRequest())))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldUpdateFinancialInfo() throws Exception {
        Long userId = stepRegister();
        stepUpdateProfile(userId);
        stepUpdateKyc(userId);

        mockMvc.perform(put("/api/v1/user/register/{userId}/financial", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildFinancialRequest())))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldCompleteFullRegistrationFlow() throws Exception {
        Long userId = stepRegister();
        stepUpdateProfile(userId);
        stepUpdateKyc(userId);
        stepUpdateFinancial(userId);
    }

    private Long stepRegister() throws Exception {
        String body = mockMvc.perform(post("/api/v1/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildRegisterRequest())))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(body).get("id").asLong();
    }

    private void stepUpdateProfile(Long userId) throws Exception {
        mockMvc.perform(put("/api/v1/user/register/{userId}/profile", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildProfileRequest())))
                .andExpect(status().isNoContent());
    }

    private void stepUpdateKyc(Long userId) throws Exception {
        mockMvc.perform(put("/api/v1/user/register/{userId}/kyc", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildKycRequest())))
                .andExpect(status().isNoContent());
    }

    private void stepUpdateFinancial(Long userId) throws Exception {
        mockMvc.perform(put("/api/v1/user/register/{userId}/financial", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildFinancialRequest())))
                .andExpect(status().isNoContent());
    }

    private UserRegisterRequest buildRegisterRequest() {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setEmail("test@test.com");
        request.setPhoneCode("+52");
        request.setPhoneNumber("1234567890");
        request.setPassword("Password123!");
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
