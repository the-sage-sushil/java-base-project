package com.sushil.base_project.user.Services;

import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.sushil.base_project.user.Entities.User;
import com.sushil.base_project.user.Models.RegistrationCommand;
import com.sushil.base_project.user.Repositories.RoleRepository;
import com.sushil.base_project.user.Repositories.UserRepository;
import com.sushil.base_project.user.UserRegistration;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService implements UserRegistration {

    private static final String USER_ROLE = "ROLE_USER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void register(RegistrationCommand command) {
        String username = command.username();
        String email = command.email();

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already registered");
        }
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username is already taken");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(command.rawPassword()));
        user.setEnabled(true);
        user.setAccountNotLocked(true);
        user.setRoles(Set.of(roleRepository.findByName(USER_ROLE)
                .orElseThrow(() -> new IllegalStateException("Required role is not configured: " + USER_ROLE))));

        userRepository.save(user);
    }
}
