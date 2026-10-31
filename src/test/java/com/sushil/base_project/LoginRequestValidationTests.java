package com.sushil.base_project;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.sushil.base_project.auth.models.LoginRequestDTO;
import com.sushil.base_project.auth.models.RegisterRequestDTO;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class LoginRequestValidationTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void loginAcceptsAnyNonBlankPassword() {
        Set<String> violations = validator.validate(new LoginRequestDTO("user@example.com", "Password"))
                .stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());

        assertThat(violations).isEmpty();
    }

    @Test
    void registrationStillRequiresStrongPassword() {
        Set<String> violations = validator.validate(
                new RegisterRequestDTO("valid_user", "user@example.com", "Password"))
                .stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());

        assertThat(violations).contains("password");
    }
}
