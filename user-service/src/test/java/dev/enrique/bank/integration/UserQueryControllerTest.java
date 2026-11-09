package dev.enrique.bank.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import dev.enrique.bank.commons.enums.Country;
import dev.enrique.bank.commons.enums.DocumentType;
import dev.enrique.bank.commons.enums.Gender;
import dev.enrique.bank.commons.enums.IncomeSource;
import dev.enrique.bank.commons.enums.MaritalStatus;
import dev.enrique.bank.commons.enums.OccupationType;
import dev.enrique.bank.commons.enums.RegisterStatus;
import dev.enrique.bank.commons.enums.UserRole;
import dev.enrique.bank.dao.UserFinancialInfoRepository;
import dev.enrique.bank.dao.UserKycRepository;
import dev.enrique.bank.dao.UserProfileRepository;
import dev.enrique.bank.dao.UserRepository;
import dev.enrique.bank.model.User;
import dev.enrique.bank.model.UserFinancialInfo;
import dev.enrique.bank.model.UserKyc;
import dev.enrique.bank.model.UserProfile;
import dev.enrique.bank.util.PostgresContainerConfig;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test-postgres")
@AutoConfigureMockMvc
class UserQueryControllerTest extends PostgresContainerConfig {
    private static final String KEYCLOAK_ID = "test-keycloak-id-123";
    private static final String EMAIL = "test@test.com";
    private static final String RFC = "MALA920101ABC";
    private static final String CURP = "MALA920101MDFRPN09";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserProfileRepository userProfileRepository;

    private static Long savedUserId;

    @BeforeAll
    static void setUp(@Autowired TestDataHelper helper) {
        savedUserId = helper.createCompleteUser();
    }

    private static RequestPostProcessor customer() {
        return customer(KEYCLOAK_ID);
    }

    private static RequestPostProcessor customer(String keycloakId) {
        return jwt().jwt(j -> j.subject(keycloakId).claim("registration_status", "COMPLETE"))
                .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER_BASIC"));
    }

    private static RequestPostProcessor admin() {
        return jwt().jwt(j -> j.subject("admin-keycloak-id").claim("registration_status", "COMPLETE"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    // ---- /me (customer, resolved from the JWT subject) ----

    @Test
    void getMe_returnsCurrentUser() throws Exception {
        mockMvc.perform(get("/api/v1/user/me").with(customer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedUserId))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.registerStatus").value("COMPLETE"));
    }

    @Test
    void getMyProfile_returnsProfileOfJwtSubject() throws Exception {
        mockMvc.perform(get("/api/v1/user/profile/me").with(customer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedUserId))
                .andExpect(jsonPath("$.names").value("Ana"));
    }

    @Test
    void getMyKyc_returnsKycOfJwtSubject() throws Exception {
        mockMvc.perform(get("/api/v1/user/kyc/me").with(customer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedUserId))
                .andExpect(jsonPath("$.rfc").value(RFC));
    }

    @Test
    void getMyFinancialInfo_returnsFinancialInfoOfJwtSubject() throws Exception {
        mockMvc.perform(get("/api/v1/user/financial-info/me").with(customer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedUserId))
                .andExpect(jsonPath("$.occupationType").value("EMPLOYEE"));
    }

    @Test
    void getMe_whenSubjectUnknown_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/user/me").with(customer("unknown-keycloak-id")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found with keycloakId: unknown-keycloak-id"));
    }

    // The production SecurityConfig and RegisterStatusFilter are the ones under test.
    @Test
    void getMe_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/user/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMe_withRegistrationInProgress_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/user/me")
                .with(jwt().jwt(j -> j.subject(KEYCLOAK_ID).claim("registration_status", "PROFILE"))
                        .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER_BASIC"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Registration incomplete"))
                .andExpect(jsonPath("$.status").value("PROFILE"));
    }

    @Test
    void actuatorBeyondHealth_isForAdminsOnly() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/metrics")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/actuator/metrics").with(customer())).andExpect(status().isForbidden());
    }

    // ---- admin endpoints ----

    @Test
    void adminEndpoints_asCustomer_returns403() throws Exception {
        for (String path : new String[] {
                "/api/v1/user/get-by-id/" + savedUserId,
                "/api/v1/user/get-by-email/" + EMAIL,
                "/api/v1/user/get-active",
                "/api/v1/user/profile/get-detail/" + savedUserId,
                "/api/v1/user/kyc/get-detail/" + savedUserId,
                "/api/v1/user/kyc/get-by-curp/" + CURP,
                "/api/v1/user/financial-info/get-detail/" + savedUserId }) {
            mockMvc.perform(get(path).with(customer())).andExpect(status().isForbidden());
        }
    }

    @Test
    void getUserById_asAdmin_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/user/get-by-id/{id}", savedUserId).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL));
    }

    @Test
    void getUserByEmail_ignoresCase() throws Exception {
        mockMvc.perform(get("/api/v1/user/get-by-email/{email}", EMAIL.toUpperCase()).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedUserId));
    }

    @Test
    void getUserByEmail_asAdmin_whenMissing_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/user/get-by-email/{email}", "nobody@test.com").with(admin()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found with email: nobody@test.com"));
    }

    @Test
    void getActiveUsers_asAdmin_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/user/get-active").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value(EMAIL))
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.size").value(20))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void getActiveUsers_paginates() throws Exception {
        mockMvc.perform(get("/api/v1/user/get-active").param("page", "1").param("size", "1").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page.number").value(1))
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.page.totalPages").value(1));
    }

    @Test
    void paginatedEndpoints_rejectInvalidPageParams() throws Exception {
        mockMvc.perform(get("/api/v1/user/get-active").param("size", "101").with(admin()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/user/profile/search/{name}", "ana").param("page", "-1").with(admin()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/user/profile/get-by-status/COMPLETE").param("size", "0").with(admin()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getProfileDetail_asAdmin_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/user/profile/get-detail/{id}", savedUserId).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.registerStatus").value("COMPLETE"));
    }

    @Test
    void searchByName_matchesFullName() throws Exception {
        mockMvc.perform(get("/api/v1/user/profile/search/{name}", "martinez lo").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(savedUserId))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void searchByName_findsProfilesWithoutSecondSurname() throws Exception {
        // The data is shared by the whole class: put the surname back afterwards
        UserProfile profile = userProfileRepository.findAll().get(0);
        profile.setSecondSurname(null);
        userProfileRepository.save(profile);
        try {
            mockMvc.perform(get("/api/v1/user/profile/search/{name}", "ana martinez").with(admin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(savedUserId))
                    .andExpect(jsonPath("$.content[0].secondSurname").doesNotExist());
        } finally {
            profile.setSecondSurname("Lopez");
            userProfileRepository.save(profile);
        }
    }

    @Test
    void getProfilesByStatus_asAdmin_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/user/profile/get-by-status/COMPLETE").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(savedUserId))
                .andExpect(jsonPath("$.page.totalElements").value(1));

        mockMvc.perform(get("/api/v1/user/profile/get-by-status/PROFILE").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    void getKycDetail_asAdmin_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/user/kyc/get-detail/{id}", savedUserId).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.curp").value(CURP))
                .andExpect(jsonPath("$.names").value("Ana"));
    }

    @Test
    void getKycByRfcAndCurp_asAdmin_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/user/kyc/get-by-rfc/{rfc}", RFC).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedUserId));

        mockMvc.perform(get("/api/v1/user/kyc/get-by-curp/{curp}", CURP).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedUserId));
    }

    @Test
    void getKycByCurp_whenMissing_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/user/kyc/get-by-curp/{curp}", "XXXX000000XXXXXX00").with(admin()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found with curp: XXXX000000XXXXXX00"));
    }

    @Test
    void getFinancialDetail_asAdmin_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/user/financial-info/get-detail/{id}", savedUserId).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyIncome").value(15000.00))
                .andExpect(jsonPath("$.email").value(EMAIL));
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        TestDataHelper testDataHelper(UserRepository userRepository,
                UserProfileRepository userProfileRepository,
                UserKycRepository userKycRepository,
                UserFinancialInfoRepository financialInfoRepository) {
            return new TestDataHelper(userRepository, userProfileRepository, userKycRepository,
                    financialInfoRepository);
        }
    }

    static class TestDataHelper {
        private final UserRepository userRepository;
        private final UserProfileRepository userProfileRepository;
        private final UserKycRepository userKycRepository;
        private final UserFinancialInfoRepository financialInfoRepository;

        TestDataHelper(UserRepository userRepository,
                UserProfileRepository userProfileRepository,
                UserKycRepository userKycRepository,
                UserFinancialInfoRepository financialInfoRepository) {
            this.userRepository = userRepository;
            this.userProfileRepository = userProfileRepository;
            this.userKycRepository = userKycRepository;
            this.financialInfoRepository = financialInfoRepository;
        }

        @Transactional
        public Long createCompleteUser() {
            financialInfoRepository.deleteAll();
            userKycRepository.deleteAll();
            userProfileRepository.deleteAll();
            userRepository.deleteAll();

            User user = new User();
            user.setEmail(EMAIL);
            user.setPhoneCode("+52");
            user.setPhoneNumber("1234567890");
            user.setActive(true);
            user.setKeycloakId(KEYCLOAK_ID);
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

            UserKyc kyc = new UserKyc();
            kyc.setUser(savedUser);
            kyc.setRfc(RFC);
            kyc.setCurp(CURP);
            kyc.setDocumentType(DocumentType.DRIVERS_LICENSE);
            userKycRepository.save(kyc);

            UserFinancialInfo financialInfo = new UserFinancialInfo();
            financialInfo.setUser(savedUser);
            financialInfo.setOccupationType(OccupationType.EMPLOYEE);
            financialInfo.setEmployerName("Acme Corp");
            financialInfo.setIncomeSource(IncomeSource.SALARY);
            financialInfo.setMonthlyIncome(new BigDecimal("15000.00"));
            financialInfo.setMaritalStatus(MaritalStatus.SINGLE);
            financialInfoRepository.save(financialInfo);

            return savedUser.getId();
        }
    }
}
