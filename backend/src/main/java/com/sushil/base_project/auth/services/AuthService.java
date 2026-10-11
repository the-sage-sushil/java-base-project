package com.sushil.base_project.auth.services;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.sushil.base_project.auth.models.LoginRequestDTO;
import com.sushil.base_project.auth.models.LoginResponseDTO;
import com.sushil.base_project.auth.models.RegisterRequestDTO;
import com.sushil.base_project.core.security.JwtService;
import com.sushil.base_project.user.RegistrationCommand;
import com.sushil.base_project.user.UserRegistration;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRegistration userRegistration;
    private final JwtService jwtService;



    public void register(RegisterRequestDTO request) {
        userRegistration.register(new RegistrationCommand(
                request.username(),
                request.email(),
                request.password()));
    }

    public LoginResponseDTO login(LoginRequestDTO request) {

        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        if (!(auth.getPrincipal() instanceof UserDetails user)) {
            throw new IllegalStateException("Authenticated principal does not provide user details");
        }

        return new LoginResponseDTO(
            jwtService.generateAccessToken(user, request.email()),
            user.getUsername()
        );

    }
}
