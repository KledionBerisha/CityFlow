package com.cityflow.route.config;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Value("${app.security.enabled:true}")
    private boolean securityEnabled;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable);

        if (!securityEnabled) {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }

        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.GET, "/routes/**").hasAuthority("ROLE_routes_read")
                        .requestMatchers(HttpMethod.POST, "/routes/**").hasAuthority("ROLE_routes_write")
                        .requestMatchers(HttpMethod.PUT, "/routes/**").hasAuthority("ROLE_routes_write")
                        .requestMatchers(HttpMethod.DELETE, "/routes/**").hasAuthority("ROLE_routes_write")
                        .requestMatchers(HttpMethod.GET, "/routes/*/stops/**").hasAuthority("ROLE_routes_read")
                        .requestMatchers(HttpMethod.PUT, "/routes/*/stops/**").hasAuthority("ROLE_routes_write")
                        .requestMatchers(HttpMethod.GET, "/routes/*/schedules/**").hasAuthority("ROLE_routes_read")
                        .requestMatchers(HttpMethod.PUT, "/routes/*/schedules/**").hasAuthority("ROLE_routes_write")
                        .requestMatchers(HttpMethod.GET, "/stops/**").hasAuthority("ROLE_stops_read")
                        .requestMatchers(HttpMethod.POST, "/stops/**").hasAuthority("ROLE_stops_write")
                        .requestMatchers(HttpMethod.PUT, "/stops/**").hasAuthority("ROLE_stops_write")
                        .requestMatchers(HttpMethod.DELETE, "/stops/**").hasAuthority("ROLE_stops_write")
                        .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

        return http.build();
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(this::extractAuthorities);
        return converter;
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        Object realmAccess = jwt.getClaims().get("realm_access");
        if (realmAccess instanceof java.util.Map<?, ?> map) {
            Object roles = map.get("roles");
            if (roles instanceof List<?> list) {
                return list.stream()
                        .filter(String.class::isInstance)
                        .map(String.class::cast)
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .collect(Collectors.toList());
            }
        }
        return List.of();
    }
}
