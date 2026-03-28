package dev.enrique.bank.integration;

import dev.enrique.bank.commons.enums.*;
import dev.enrique.bank.dao.UserFinancialInfoRepository;
import dev.enrique.bank.dao.UserProfileRepository;
import dev.enrique.bank.dao.UserRepository;
import dev.enrique.bank.model.User;
import dev.enrique.bank.model.UserFinancialInfo;
import dev.enrique.bank.model.UserProfile;
import dev.enrique.bank.util.PostgresContainerConfig;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test-postgres")
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserFinancialInfoControllerTest extends PostgresContainerConfig {
    @Autowired
    private MockMvc mockMvc;

    private static Long savedUserId;

    @BeforeAll
    static void setUp(@Autowired TestDataHelper helper) {
        savedUserId = helper.createUserWithFinancialInfo();
    }

    @Test
    void getFinancialInfoByEmail_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/user/financial-info/get-by-user-id/{userId}", savedUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedUserId));
    }

    @Test
    void getFinancialInfoDetail_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/user/financial-info/get-detail/{userId}", savedUserId))
                .andExpect(status().isOk());
    }

    @Test
    void getByOccupationType_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/user/financial-info/get-by-occupation/EMPLOYEE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0]").exists());
    }

    @Test
    void getByIncomeSource_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/user/financial-info/get-by-income-source/SALARY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0]").exists());
    }

    @Test
    void getByMaritalStatus_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/user/financial-info/get-by-marital-status/SINGLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0]").exists());
    }

    @Test
    void getByIncomeRange_whenInRange_returns200WithResults() throws Exception {
        mockMvc.perform(get("/api/v1/user/financial-info/get-by-income-range")
                        .param("min", "10000")
                        .param("max", "20000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0]").exists());
    }

    @Test
    void getByIncomeRange_whenOutOfRange_returns200WithEmptyList() throws Exception {
        mockMvc.perform(get("/api/v1/user/financial-info/get-by-income-range")
                        .param("min", "1000")
                        .param("max", "5000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getAllFinancialInfoDetailed_returns200WithResults() throws Exception {
        mockMvc.perform(get("/api/v1/user/financial-info/get-all-detailed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0]").exists());
    }

    @Test
    void getActiveUsers_returns200WithResults() throws Exception {
        mockMvc.perform(get("/api/v1/user/financial-info/get-active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0]").exists());
    }

    @Test
    void existsFinancialInfo_whenExists_returnsTrue() throws Exception {
        mockMvc.perform(get("/api/v1/user/financial-info/exists/{userId}", savedUserId))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        TestDataHelper testDataHelper(UserRepository userRepository,
                                      UserFinancialInfoRepository financialInfoRepository,
                                      UserProfileRepository userProfileRepository) {
            return new TestDataHelper(userRepository, financialInfoRepository, userProfileRepository);
        }
    }

    @Component
    static class TestDataHelper {
        private final UserRepository userRepository;
        private final UserProfileRepository userProfileRepository;
        private final UserFinancialInfoRepository financialInfoRepository;

        TestDataHelper(UserRepository userRepository,
                       UserFinancialInfoRepository financialInfoRepository,
                       UserProfileRepository userProfileRepository) {
            this.userRepository = userRepository;
            this.userProfileRepository = userProfileRepository;
            this.financialInfoRepository = financialInfoRepository;
        }

        @Transactional
        public Long createUserWithFinancialInfo() {
            financialInfoRepository.deleteAll();
            userProfileRepository.deleteAll();
            userRepository.deleteAll();

            User user = new User();
            user.setEmail("test@test.com");
            user.setPhoneCode("+52");
            user.setPhoneNumber("1234567890");
            user.setActive(true);
            user.setKeycloakId("test-keycloak-id-123");
            user.setRole(UserRole.CUSTOMER_BASIC);
            user.setRegisterStatus(RegisterStatus.COMPLETE);
            User savedUser = userRepository.save(user);

            UserProfile profile = new UserProfile();
            profile.setUser(savedUser);
            profile.setNames("Ana");
            profile.setFirstSurname("Martinez");
            profile.setSecondSurname("Lopez");
            profile.setGender(Gender.FEMALE);
            profile.setCountryOfBirth(Country.MX);
            userProfileRepository.save(profile);

            UserFinancialInfo financialInfo = new UserFinancialInfo();
            financialInfo.setUser(savedUser);
            financialInfo.setOccupationType(OccupationType.EMPLOYEE);
            financialInfo.setIncomeSource(IncomeSource.SALARY);
            financialInfo.setMonthlyIncome(new BigDecimal("15000.00"));
            financialInfo.setMaritalStatus(MaritalStatus.SINGLE);
            financialInfoRepository.save(financialInfo);

            return savedUser.getId();
        }
    }
}
