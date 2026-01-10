package com.cityflow.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Value("${app.security.enabled:true}")
    private boolean securityEnabled;

    @Bean
    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
        http.csrf(ServerHttpSecurity.CsrfSpec::disable);

        if (!securityEnabled) {
            http.authorizeExchange(exchange -> exchange.anyExchange().permitAll());
            return http.build();
        }

        http.authorizeExchange(exchange -> {
                    // Public paths
                    exchange.pathMatchers("/actuator/health", "/actuator/info").permitAll();
                    
                    // Auth endpoints - public for registration, authenticated for password change
                    exchange.pathMatchers(HttpMethod.POST, "/api/auth/register").permitAll();
                    exchange.pathMatchers(HttpMethod.POST, "/api/auth/change-password").authenticated();
                    
                    // Bus Service - Read operations
                    exchange.pathMatchers(HttpMethod.GET, "/api/buses/**").hasAuthority("ROLE_bus_read");
                    exchange.pathMatchers(HttpMethod.GET, "/api/bus-locations/**").hasAuthority("ROLE_bus_read");
                    
                    // Bus Service - Write operations
                    exchange.pathMatchers(HttpMethod.POST, "/api/buses/**").hasAuthority("ROLE_bus_write");
                    exchange.pathMatchers(HttpMethod.PUT, "/api/buses/**").hasAuthority("ROLE_bus_write");
                    exchange.pathMatchers(HttpMethod.PATCH, "/api/buses/**").hasAuthority("ROLE_bus_write");
                    exchange.pathMatchers(HttpMethod.DELETE, "/api/buses/**").hasAuthority("ROLE_bus_write");
                    
                    // Route Service - Read operations
                    exchange.pathMatchers(HttpMethod.GET, "/api/routes/**").hasAuthority("ROLE_routes_read");
                    exchange.pathMatchers(HttpMethod.GET, "/api/stops/**").hasAuthority("ROLE_stops_read");
                    
                    // Route Service - Write operations
                    exchange.pathMatchers(HttpMethod.POST, "/api/routes/**").hasAuthority("ROLE_routes_write");
                    exchange.pathMatchers(HttpMethod.PUT, "/api/routes/**").hasAuthority("ROLE_routes_write");
                    exchange.pathMatchers(HttpMethod.DELETE, "/api/routes/**").hasAuthority("ROLE_routes_write");
                    exchange.pathMatchers(HttpMethod.POST, "/api/stops/**").hasAuthority("ROLE_stops_write");
                    exchange.pathMatchers(HttpMethod.PUT, "/api/stops/**").hasAuthority("ROLE_stops_write");
                    exchange.pathMatchers(HttpMethod.DELETE, "/api/stops/**").hasAuthority("ROLE_stops_write");
                    
                    // All other requests must be authenticated
                    exchange.anyExchange().authenticated();
                })
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

        return http.build();
    }

    private Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setPrincipalClaimName("preferred_username");
        
        // Extract roles from Keycloak's realm_access.roles
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            var realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess != null && realmAccess.containsKey("roles")) {
                var roles = (List<String>) realmAccess.get("roles");
                return roles.stream()
                        .map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role))
                        .collect(java.util.stream.Collectors.toList());
            }
            return java.util.Collections.emptyList();
        });

        return new ReactiveJwtAuthenticationConverterAdapter(converter);
    }
}
