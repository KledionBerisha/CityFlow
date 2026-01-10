package com.cityflow.route.service;

import com.cityflow.route.dto.RegisterRequest;
import jakarta.ws.rs.core.Response;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final Keycloak keycloakAdmin;
    private final String realm;

    public AuthService(Keycloak keycloakAdmin, @Value("${keycloak.realm:cityflow}") String realm) {
        this.keycloakAdmin = keycloakAdmin;
        this.realm = realm;
    }

    public void registerUser(RegisterRequest request) {
        // Create user representation
        UserRepresentation user = new UserRepresentation();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEnabled(true);
        user.setEmailVerified(true);

        // Create user in Keycloak
        Response response = keycloakAdmin.realm(realm)
            .users()
            .create(user);

        if (response.getStatus() != 201) {
            String errorMessage = response.readEntity(String.class);
            response.close();
            throw new RuntimeException("Failed to create user: " + errorMessage);
        }

        // Get the created user's ID from the location header
        String userId = extractUserIdFromLocation(response.getLocation().getPath());
        response.close();

        // Set password
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(request.password());
        credential.setTemporary(false);

        keycloakAdmin.realm(realm)
            .users()
            .get(userId)
            .resetPassword(credential);
    }

    private String extractUserIdFromLocation(String locationPath) {
        // Location path format: /admin/realms/{realm}/users/{userId}
        String[] parts = locationPath.split("/");
        return parts[parts.length - 1];
    }
}
