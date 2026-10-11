package com.sushil.base_project.auth.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginRequestDTO(

        @NotBlank
        @Pattern(
                regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", 
                message = "Please provide a valid email address"
        ) 
        String email,

        @NotBlank
        String password
) {
}
