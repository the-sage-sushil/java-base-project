package com.sushil.base_project.user.Models;

public record RegistrationCommand(String username, String email, String rawPassword) {
}
