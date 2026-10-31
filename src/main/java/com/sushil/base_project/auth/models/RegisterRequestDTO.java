package com.sushil.base_project.auth.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RegisterRequestDTO(

        @NotBlank
        @Pattern(
                regexp = "^[a-zA-Z0-9_]{5,}$",
                message = "Username must be at least 5 characters long and contain only letters, numbers, and underscores")
        String username,

        @NotBlank
        @Pattern(
                regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
                message = "Please provide a valid email address")
        String email,

        @NotBlank
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
                message = "Password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, one digit, and one special character")
        String password) {
}
