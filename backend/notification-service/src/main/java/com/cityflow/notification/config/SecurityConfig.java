package com.cityflow.notification.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

    @Value("${app.security.enabled:true}")
    private boolean securityEnabled;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        if (!securityEnabled) {
            return http
                    .csrf(ServerHttpSecurity.CsrfSpec::disable)
                    .authorizeExchange(auth -> auth.anyExchange().permitAll())
                    .build();
        }

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(auth -> auth
                        // Public endpoints
                        .pathMatchers("/actuator/health", "/actuator/info").permitAll()
                        .pathMatchers("/ws/**").permitAll()  // WebSocket endpoint
                        
                        // Notification endpoints
                        .pathMatchers(HttpMethod.GET, "/notifications/**").hasAnyAuthority("SCOPE_notification_read", "ROLE_notification_read")
                        .pathMatchers(HttpMethod.PATCH, "/notifications/**").hasAnyAuthority("SCOPE_notification_write", "ROLE_notification_write")
                        .pathMatchers(HttpMethod.DELETE, "/notifications/**").hasAnyAuthority("SCOPE_notification_write", "ROLE_notification_write")
                        
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}))
                .build();
    }
}
