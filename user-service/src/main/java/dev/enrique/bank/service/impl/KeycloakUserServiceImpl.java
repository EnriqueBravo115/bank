package dev.enrique.bank.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import dev.enrique.bank.commons.enums.UserRole;
import dev.enrique.bank.commons.dto.request.UserProfileRequest;
import dev.enrique.bank.commons.dto.request.UserRegisterRequest;
import dev.enrique.bank.service.KeycloakUserService;
import jakarta.ws.rs.core.Response;

@Service
public class KeycloakUserServiceImpl implements KeycloakUserService {
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
