package com.cityflow.route.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "Username is required")
    String username,
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,
    
    @NotBlank(message = "First name is required")
    String firstName,
    
    String lastName,
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    String password
) {}
