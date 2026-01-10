package com.cityflow.route.config;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KeycloakAdminConfig {

    @Value("${keycloak.auth-server-url:http://localhost:8080}")
    private String authServerUrl;

    @Value("${keycloak.realm:cityflow}")
    private String realm;

    @Value("${keycloak.admin-client-id:admin-cli}")
    private String adminClientId;

    @Value("${keycloak.admin-username:admin}")
    private String adminUsername;

    @Value("${keycloak.admin-password:admin}")
    private String adminPassword;

    @Bean
    @ConditionalOnProperty(name = "keycloak.enabled", havingValue = "true", matchIfMissing = true)
    public Keycloak keycloakAdmin() {
        try {
            Keycloak keycloak = KeycloakBuilder.builder()
                .serverUrl(authServerUrl)
                .realm("master") // Admin client uses master realm
                .clientId(adminClientId)
                .username(adminUsername)
                .password(adminPassword)
                .build();
            
            // Test connection
            keycloak.realms().findAll();
            return keycloak;
        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to initialize Keycloak admin client. " +
                "Make sure Keycloak is running at " + authServerUrl + ". " +
                "You can disable Keycloak by setting keycloak.enabled=false", e);
        }
    }

    @Bean
    public String keycloakRealm() {
        return realm;
    }
}
