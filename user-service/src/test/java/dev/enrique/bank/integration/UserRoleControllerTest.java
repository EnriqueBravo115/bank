package dev.enrique.bank.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import dev.enrique.bank.broker.producer.UserEventProducer;
import dev.enrique.bank.commons.dto.request.UserRegisterRequest;
import dev.enrique.bank.commons.enums.RegisterStatus;
import dev.enrique.bank.commons.enums.UserRole;
import dev.enrique.bank.dao.UserFinancialInfoRepository;
import dev.enrique.bank.dao.UserKycRepository;
import dev.enrique.bank.dao.UserProfileRepository;
import dev.enrique.bank.dao.UserRepository;
import dev.enrique.bank.model.User;
import dev.enrique.bank.service.KeycloakUserService;
import dev.enrique.bank.util.FullContainerConfig;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test-keycloak")
class UserRoleControllerTest extends FullContainerConfig {
    private static final String EMAIL = "role@test.com";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserProfileRepository userProfileRepository;
    @Autowired
    private UserKycRepository userKycRepository;
    @Autowired
    private UserFinancialInfoRepository userFinancialInfoRepository;
    @MockitoSpyBean
    private KeycloakUserService keycloakUserService;
    @Autowired
    private Keycloak keycloak;
    @Value("${keycloak.realm}")
    private String realm;

    @MockitoBean
    private UserEventProducer userEventProducer;

    @BeforeEach
    void cleanUp() {
        userFinancialInfoRepository.deleteAll();
        userKycRepository.deleteAll();
        userProfileRepository.deleteAll();
        userRepository.deleteAll();
        keycloak.realm(realm).users().searchByEmail(EMAIL, true)
                .forEach(user -> keycloak.realm(realm).users().get(user.getId()).remove());
    }

    @Test
    void shouldPromoteCustomerToPremium() throws Exception {
        User user = createUser(RegisterStatus.COMPLETE);

        changeRole(user.getId(), "CUSTOMER_PREMIUM", admin())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.role").value("CUSTOMER_PREMIUM"));

        assertThat(roleInDb(user)).isEqualTo(UserRole.CUSTOMER_PREMIUM);
        assertThat(keycloakRoles(user)).contains("CUSTOMER_BASIC", "CUSTOMER_PREMIUM");
    }

    @Test
    void shouldReplacePreviousTierWhenChangingTier() throws Exception {
        User user = createUser(RegisterStatus.COMPLETE);
        changeRole(user.getId(), "CUSTOMER_PREMIUM", admin()).andExpect(status().isOk());

        changeRole(user.getId(), "CUSTOMER_BUSINESS", admin()).andExpect(status().isOk());

        assertThat(roleInDb(user)).isEqualTo(UserRole.CUSTOMER_BUSINESS);
        assertThat(keycloakRoles(user))
                .contains("CUSTOMER_BASIC", "CUSTOMER_BUSINESS")
                .doesNotContain("CUSTOMER_PREMIUM");
    }

    @Test
    void shouldDemoteBackToBasic() throws Exception {
        User user = createUser(RegisterStatus.KYC);
        changeRole(user.getId(), "CUSTOMER_BUSINESS", admin()).andExpect(status().isOk());

        changeRole(user.getId(), "CUSTOMER_BASIC", admin()).andExpect(status().isOk());

        assertThat(roleInDb(user)).isEqualTo(UserRole.CUSTOMER_BASIC);
        assertThat(keycloakRoles(user))
                .contains("CUSTOMER_BASIC")
                .doesNotContain("CUSTOMER_PREMIUM", "CUSTOMER_BUSINESS");
    }

    @Test
    void shouldRestoreKeycloakTierWhenRoleChangeFails() throws Exception {
        User user = createUser(RegisterStatus.COMPLETE);
        // Keycloak applies the change, but the request still fails (e.g. the response is lost).
        doAnswer(invocation -> {
            invocation.callRealMethod();
            throw new IllegalStateException("Keycloak down");
        }).when(keycloakUserService).setCustomerTier(user.getKeycloakId(), UserRole.CUSTOMER_PREMIUM);

        assertThatThrownBy(() -> changeRole(user.getId(), "CUSTOMER_PREMIUM", admin()))
                .hasRootCauseMessage("Keycloak down");

        assertThat(roleInDb(user)).isEqualTo(UserRole.CUSTOMER_BASIC);
        assertThat(keycloakRoles(user))
                .contains("CUSTOMER_BASIC")
                .doesNotContain("CUSTOMER_PREMIUM");
    }

    @Test
    void shouldRejectGrantingAdmin() throws Exception {
        User user = createUser(RegisterStatus.COMPLETE);

        changeRole(user.getId(), "ADMIN", admin())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("ADMIN role cannot be granted through the API"));

        assertThat(roleInDb(user)).isEqualTo(UserRole.CUSTOMER_BASIC);
        assertThat(keycloakRoles(user)).doesNotContain("ADMIN");
    }

    @Test
    void shouldRejectUnknownRole() throws Exception {
        User user = createUser(RegisterStatus.COMPLETE);

        changeRole(user.getId(), "SUPER_USER", admin()).andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectUserWithoutKyc() throws Exception {
        User user = createUser(RegisterStatus.PROFILE);

        changeRole(user.getId(), "CUSTOMER_PREMIUM", admin()).andExpect(status().isConflict());

        assertThat(roleInDb(user)).isNull();
    }

    @Test
    void shouldReturnNotFoundForUnknownUser() throws Exception {
        changeRole(999_999L, "CUSTOMER_PREMIUM", admin())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found with id: 999999"));
    }

    @Test
    void shouldForbidCustomers() throws Exception {
        User user = createUser(RegisterStatus.COMPLETE);

        changeRole(user.getId(), "CUSTOMER_PREMIUM", customer(user.getKeycloakId()))
                .andExpect(status().isForbidden());

        assertThat(roleInDb(user)).isEqualTo(UserRole.CUSTOMER_BASIC);
    }

    private ResultActions changeRole(Long userId, String role, RequestPostProcessor auth) throws Exception {
        return mockMvc.perform(put("/api/v1/user/{userId}/role", userId)
                .with(auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\": \"" + role + "\"}"));
    }

    // Same state the registration flow leaves: Keycloak user plus DB row; role granted from KYC on.
    private User createUser(RegisterStatus status) {
        UserRegisterRequest request = new UserRegisterRequest();
        request.setEmail(EMAIL);
        request.setPassword("Password123!");
        request.setPhoneCode("+52");
        request.setPhoneNumber("5512345678");
        String keycloakId = keycloakUserService.createUser(request);

        boolean kycDone = status == RegisterStatus.KYC || status == RegisterStatus.COMPLETE;
        if (kycDone) {
            keycloakUserService.assignRole(keycloakId, UserRole.CUSTOMER_BASIC);
        }

        User user = new User();
        user.setKeycloakId(keycloakId);
        user.setEmail(EMAIL);
        user.setPhoneCode("+52");
        user.setPhoneNumber("5512345678");
        user.setActive(true);
        user.setRegisterStatus(status);
        user.setRole(kycDone ? UserRole.CUSTOMER_BASIC : null);
        return userRepository.save(user);
    }

    private UserRole roleInDb(User user) {
        return userRepository.findById(user.getId()).orElseThrow().getRole();
    }

    private List<String> keycloakRoles(User user) {
        return keycloak.realm(realm).users().get(user.getKeycloakId()).roles().realmLevel().listAll().stream()
                .map(RoleRepresentation::getName)
                .toList();
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
