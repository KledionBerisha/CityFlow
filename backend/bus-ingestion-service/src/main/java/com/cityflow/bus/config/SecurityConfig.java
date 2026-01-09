package com.cityflow.bus.config;

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

        http.authorizeExchange(exchange -> exchange
                        .pathMatchers("/actuator/health", "/actuator/info").permitAll()
                        // Bus read operations
                        .pathMatchers(HttpMethod.GET, "/buses/**").hasAuthority("ROLE_bus_read")
                        .pathMatchers(HttpMethod.GET, "/bus-locations/**").hasAuthority("ROLE_bus_read")
                        // Bus write operations
                        .pathMatchers(HttpMethod.POST, "/buses/**").hasAuthority("ROLE_bus_write")
                        .pathMatchers(HttpMethod.PUT, "/buses/**").hasAuthority("ROLE_bus_write")
                        .pathMatchers(HttpMethod.PATCH, "/buses/**").hasAuthority("ROLE_bus_write")
                        .pathMatchers(HttpMethod.DELETE, "/buses/**").hasAuthority("ROLE_bus_write")
                        .anyExchange().authenticated())
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
                var roles = (java.util.List<String>) realmAccess.get("roles");
                return roles.stream()
                        .map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role))
                        .collect(java.util.stream.Collectors.toList());
            }
            return java.util.Collections.emptyList();
        });

        return new ReactiveJwtAuthenticationConverterAdapter(converter);
    }
}
