package com.cityflow.route.service;

import com.cityflow.route.dto.ChangePasswordRequest;
import com.cityflow.route.dto.RegisterRequest;
import jakarta.ws.rs.core.Response;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AuthService {

    private final Keycloak keycloakAdmin;
    private final String realm;
    private final String authServerUrl;

    public AuthService(
            Keycloak keycloakAdmin, 
            @Value("${keycloak.realm:cityflow}") String realm,
            @Value("${keycloak.auth-server-url:http://localhost:8080}") String authServerUrl) {
        this.keycloakAdmin = keycloakAdmin;
        this.realm = realm;
        this.authServerUrl = authServerUrl;
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

    public void changePassword(String userEmail, ChangePasswordRequest request) {
        // Find user by email (try both email field and username field)
        List<UserRepresentation> users = keycloakAdmin.realm(realm)
            .users()
            .search(userEmail, true);

        // If not found by exact email, try searching by username
        if (users.isEmpty()) {
            users = keycloakAdmin.realm(realm)
                .users()
                .searchByUsername(userEmail, true);
        }
        
        // If still not found, try a broader search
        if (users.isEmpty()) {
            users = keycloakAdmin.realm(realm)
                .users()
                .search(userEmail);
            // Filter to exact match
            String searchEmail = userEmail.toLowerCase();
            users = users.stream()
                .filter(u -> (u.getEmail() != null && u.getEmail().equalsIgnoreCase(searchEmail)) 
                          || (u.getUsername() != null && u.getUsername().equalsIgnoreCase(searchEmail)))
                .toList();
        }

        if (users.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with email/username: " + userEmail);
        }

        UserRepresentation user = users.get(0);

        // Verify current password by attempting to get a token
        try {
            Keycloak userClient = KeycloakBuilder.builder()
                .serverUrl(authServerUrl)
                .realm(realm)
                .clientId("cityflow-frontend")
                .username(userEmail)
                .password(request.currentPassword())
                .build();
            
            // Test the credentials
            userClient.tokenManager().getAccessToken();
            userClient.close();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current password is incorrect");
        }

        // Set new password
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(request.newPassword());
        credential.setTemporary(false);

        keycloakAdmin.realm(realm)
            .users()
            .get(user.getId())
            .resetPassword(credential);
    }
}
