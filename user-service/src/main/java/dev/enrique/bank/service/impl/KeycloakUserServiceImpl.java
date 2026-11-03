package dev.enrique.bank.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import dev.enrique.bank.commons.enums.UserRole;
import dev.enrique.bank.commons.exception.DuplicateUserDataException;
import dev.enrique.bank.commons.dto.request.UserProfileRequest;
import dev.enrique.bank.commons.dto.request.UserRegisterRequest;
import dev.enrique.bank.service.KeycloakUserService;
import jakarta.ws.rs.core.Response;

@Service
public class KeycloakUserServiceImpl implements KeycloakUserService {
    private static final Set<String> TIER_ROLES = Set.of(
            UserRole.CUSTOMER_PREMIUM.name(),
            UserRole.CUSTOMER_BUSINESS.name());

    private final Keycloak keycloak;
    private final String realm;

    public KeycloakUserServiceImpl(
            Keycloak keycloak,
            @Value("${keycloak.realm}") String realm) {
        this.keycloak = keycloak;
        this.realm = realm;
    }

    public String createUser(UserRegisterRequest request) {
        UserRepresentation user = new UserRepresentation();

        user.setUsername(request.getEmail());
        user.setFirstName("PENDING");
        user.setLastName("PENDING");
        user.setEmail(request.getEmail());
        user.setEmailVerified(true);
        user.setEnabled(true);
        user.setRequiredActions(List.of());

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(request.getPassword());
        credential.setTemporary(false);

        user.setCredentials(List.of(credential));

        RealmResource realmResource = keycloak.realm(realm);
        Response response = realmResource.users().create(user);

        if (response.getStatus() == 201) {
            String location = response.getHeaderString("Location");
            return location.substring(location.lastIndexOf("/") + 1);
        }

        // Keycloak-only users (e.g. admins created in the console) are not in user_db.
        if (response.getStatus() == 409) {
            throw new DuplicateUserDataException("email");
        }

        throw new RuntimeException("Error user creation in Keycloak: " + response.getStatus());
    }

    @Override
    public void updateUser(String keycloakId, UserProfileRequest request) {
        UserRepresentation user = new UserRepresentation();
        user.setFirstName(request.getNames());
        user.setLastName(request.getFirstSurname() + " " + request.getSecondSurname());

        keycloak.realm(realm)
                .users()
                .get(keycloakId)
                .update(user);
    }

    @Override
    public void deleteUser(String keycloakId) {
        Response response = keycloak.realm(realm)
                .users()
                .delete(keycloakId);

        if (response.getStatus() != 204) {
            throw new RuntimeException("Error deleting user in Keycloak: " + response.getStatus());
        }
    }

    @Override
    public void assignRole(String keycloakId, UserRole roleName) {
        RealmResource realmResource = keycloak.realm(realm);

        RoleRepresentation role = realmResource.roles()
                .get(roleName.toString())
                .toRepresentation();

        realmResource.users()
                .get(keycloakId)
                .roles()
                .realmLevel()
                .add(List.of(role));
    }

    // CUSTOMER_BASIC is the base customer role every endpoint checks, so it is always kept;
    // PREMIUM/BUSINESS are tiers on top of it and only one of them can be active.
    @Override
    public void setCustomerTier(String keycloakId, UserRole tier) {
        RealmResource realmResource = keycloak.realm(realm);
        RoleScopeResource userRealmRoles = realmResource.users().get(keycloakId).roles().realmLevel();
        List<RoleRepresentation> currentRoles = userRealmRoles.listAll();

        List<RoleRepresentation> toRemove = currentRoles.stream()
                .filter(role -> TIER_ROLES.contains(role.getName()) && !role.getName().equals(tier.name()))
                .toList();
        if (!toRemove.isEmpty()) {
            userRealmRoles.remove(toRemove);
        }

        Set<String> currentNames = currentRoles.stream()
                .map(RoleRepresentation::getName)
                .collect(Collectors.toSet());
        List<RoleRepresentation> toAdd = Stream.of(UserRole.CUSTOMER_BASIC.name(), tier.name())
                .distinct()
                .filter(name -> !currentNames.contains(name))
                .map(name -> realmResource.roles().get(name).toRepresentation())
                .toList();
        if (!toAdd.isEmpty()) {
            userRealmRoles.add(toAdd);
        }
    }

    @Override
    public void updateRegistrationStatus(String keycloakId, String status) {
        UserResource userResource = keycloak.realm(realm).users().get(keycloakId);
        UserRepresentation user = userResource.toRepresentation();
        Map<String, List<String>> attributes = user.getAttributes();

        if (attributes == null)
            attributes = new HashMap<>();

        attributes.put("registration_status", List.of(status));
        user.setAttributes(attributes);
        userResource.update(user);
    }
}
