package com.sushil.base_project.user;

public record RegistrationCommand(String username, String email, String rawPassword) {
}
