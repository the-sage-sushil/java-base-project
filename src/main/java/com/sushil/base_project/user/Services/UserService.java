package com.sushil.base_project.user.Services;

import java.util.Set;
import java.util.Locale;
import java.util.Optional;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sushil.base_project.core.common.exceptions.DuplicateResourceException;
import com.sushil.base_project.user.RegistrationCommand;
import com.sushil.base_project.user.UserAccountSecurity;
import com.sushil.base_project.user.UserRegistration;
import com.sushil.base_project.user.Entities.User;
import com.sushil.base_project.user.Repositories.RoleRepository;
import com.sushil.base_project.user.Repositories.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService implements UserRegistration, UserAccountSecurity {

    private static final String USER_ROLE = "ROLE_USER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void register(RegistrationCommand command) {
        String username = command.username();
        String email = normalizeEmail(command.email());

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email is already registered");
        }
        if (userRepository.existsByUsername(username)) {
            throw new DuplicateResourceException("Username is already taken");
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

    @Override
    @Transactional
    public void recordFailedLogin(String email, int maxAttempts) {
        findByEmail(email).ifPresent(user -> {
            int limit = Math.max(maxAttempts, 1);
            int failedAttempts = user.getFailedLoginAttempts();
            if (failedAttempts < limit) {
                failedAttempts++;
            }
            user.setFailedLoginAttempts(failedAttempts);
            if (failedAttempts >= limit) {
                user.setAccountNotLocked(false);
            }
        });
    }

    @Override
    @Transactional
    public void recordSuccessfulLogin(String email) {
        findByEmail(email).ifPresent(user -> user.setFailedLoginAttempts(0));
    }

    @Override
    @Transactional
    public void changePassword(String email, String currentPassword, String newPassword) {
        User user = findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid current password"));
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BadCredentialsException("Invalid current password");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setFailedLoginAttempts(0);
    }

    private Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(normalizeEmail(email));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
