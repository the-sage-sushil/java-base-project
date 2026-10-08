package com.sushil.base_project.user.Services;

import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.sushil.base_project.auth.models.LoginRequestDTO;
import com.sushil.base_project.auth.models.LoginResponseDTO;
import com.sushil.base_project.user.Entities.User;
import com.sushil.base_project.user.Models.RegisterRequestDTO;
import com.sushil.base_project.user.Repositories.RoleRepository;
import com.sushil.base_project.user.Repositories.UserRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String USER_ROLE = "ROLE_USER";
    private static final String ADMIN_ROLE = "ROLE_ADMIN";

    public void createUser(RegisterRequestDTO request) {

        if (ifEmailPresent(request.email())) {
            throw new RuntimeException("This email is already present");
        }
        if (ifUserNamePresent(request.username())) {
            throw new RuntimeException("This username is already present");
        }
        if (request.password().isBlank()) {
            throw new IllegalArgumentException("Password must not be blank");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(encriptPassword(request.password()));
        user.setEnabled(true);
        user.setAccountNotLocked(true);
        user.setRoles(roleRepository.findByName(USER_ROLE).map(Set::of)
                .orElseThrow(() -> new RuntimeException("Error: Role is not found.")));

        userRepository.save(user);
    }

    private boolean ifUserNamePresent(String username) {
        return userRepository.existsByUsername(username);
    }

    private boolean ifEmailPresent(String email) {
        return userRepository.existsByEmail(email);
    }

    private String encriptPassword(String password) {
        return passwordEncoder.encode(password);
    }

    public User login(LoginRequestDTO request) {

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException(
                        "No user found with the following email {{request.email}}"));

        if (!passwordEncoder.matches(request.password(),user.getPassword())) throw new RuntimeException("Email or passeord is incorrct");
        
        return user;
    }

}
