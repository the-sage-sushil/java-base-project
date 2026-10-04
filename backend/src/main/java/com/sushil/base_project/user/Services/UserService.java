package com.sushil.base_project.user.Services;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.sushil.base_project.user.Models.RegisterRequestDTO;
import com.sushil.base_project.user.Repositories.UserRepository;

/**
 * UserService
 */
public class UserService {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;

    
    public void createUser(RegisterRequestDTO request) {
        
        if (!ifEmailPresent(request.email()) && !ifUserNamePresent(request.username()) && !request.password().isBlank()){
            
        }

        throw new UnsupportedOperationException("Unimplemented method 'createUser'");
    }
    
    private boolean ifUserNamePresent(String username) {
        return userRepository.existsByUsername(username);
    }
    
    private boolean ifEmailPresent(String email) {
        return userRepository.existsByEmail(email);
    }
    
    private String encriptPassword(String password){
        return passwordEncoder.encode(password);
    }
    
    private String decriptPassword(String password){
        return passwordEncoder.
    }
}
