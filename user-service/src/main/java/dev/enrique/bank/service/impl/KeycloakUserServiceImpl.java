package dev.enrique.bank.service.impl;

import java.util.List;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import dev.enrique.bank.dto.request.UserRegisterRequest;
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
        user.setEmail(request.getEmail());
        user.setEmailVerified(true);
        user.setEnabled(true);
        user.setRequiredActions(List.of());

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(request.getPassword());
        credential.setTemporary(false);
        credential.setCredentialData("{\"hashIterations\":275000,\"algorithm\":\"pbkdf2-sha256\"}");
        credential.setSecretData("{\"value\":\"" + request.getPassword() + "\",\"additionalParameters\":{}}");

        user.setCredentials(List.of(credential));

        RealmResource realmResource = keycloak.realm(realm);
        Response response = realmResource.users().create(user);

        String responseBody = response.readEntity(String.class);
        System.out.println(">>> Keycloak response status: " + response.getStatus());
        System.out.println(">>> Keycloak response body: " + responseBody);

        if (response.getStatus() == 201) {
            String location = response.getHeaderString("Location");
            return location.substring(location.lastIndexOf("/") + 1);
        }

        throw new RuntimeException("Error user creation in Keycloak: " + response.getStatus());
    }
}
