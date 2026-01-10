package com.cityflow.route.web;

import com.cityflow.route.dto.ChangePasswordRequest;
import com.cityflow.route.dto.RegisterRequest;
import com.cityflow.route.service.AuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> register(@Valid @RequestBody RegisterRequest request) {
        authService.registerUser(request);
        return Map.of("message", "User registered successfully");
    }

    @PostMapping("/change-password")
    public Map<String, String> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                              Authentication authentication) {
        log.debug("Change password request received. Auth present: {}, Request email: {}", 
                  authentication != null, request.email());
        String userEmail = resolveUserEmail(authentication, request);
        log.info("Resolved user email for password change: {}", userEmail);
        authService.changePassword(userEmail, request);
        return Map.of("message", "Password changed successfully");
    }

    private String resolveUserEmail(Authentication authentication, ChangePasswordRequest request) {
        // First try to get email from JWT token
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            // Try email claim first
            String email = jwt.getClaim("email");
            if (email != null && !email.isBlank()) {
                return email;
            }
            // Fall back to preferred_username (often contains email)
            String preferredUsername = jwt.getClaim("preferred_username");
            if (preferredUsername != null && !preferredUsername.isBlank()) {
                return preferredUsername;
            }
            // Try sub claim as last resort from JWT
            String sub = jwt.getClaim("sub");
            if (sub != null && !sub.isBlank() && sub.contains("@")) {
                return sub;
            }
        }

        // Fall back to email from request body
        if (request.email() != null && !request.email().isBlank()) {
            return request.email();
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unable to determine user identity. Please ensure your token contains email claim or provide email in request.");
    }
}
