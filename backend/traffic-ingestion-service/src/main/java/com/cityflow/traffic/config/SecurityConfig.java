package com.cityflow.traffic.config;

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
            // Disable security for development/testing
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
                        
                        // Sensor endpoints
                        .pathMatchers(HttpMethod.GET, "/sensors/**").hasAnyAuthority("SCOPE_traffic_read", "ROLE_traffic_read")
                        .pathMatchers(HttpMethod.POST, "/sensors").hasAnyAuthority("SCOPE_traffic_write", "ROLE_traffic_write")
                        .pathMatchers(HttpMethod.PATCH, "/sensors/**").hasAnyAuthority("SCOPE_traffic_write", "ROLE_traffic_write")
                        .pathMatchers(HttpMethod.DELETE, "/sensors/**").hasAnyAuthority("SCOPE_traffic_write", "ROLE_traffic_write")
                        
                        // Traffic reading endpoints (mostly read)
                        .pathMatchers("/traffic/**").hasAnyAuthority("SCOPE_traffic_read", "ROLE_traffic_read")
                        
                        // All other requests require authentication
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}))
                .build();
    }
}
