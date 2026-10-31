package com.sushil.base_project.auth.services;

import java.util.Optional;

import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.sushil.base_project.auth.models.AuthTokens;
import com.sushil.base_project.auth.models.ClientInfo;
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
    private final RefreshTokenService refreshTokenService;
    private final UserDetailsService userDetailsService;

    public void register(RegisterRequestDTO request) {
        userRegistration.register(new RegistrationCommand(
                request.username(),
                request.email(),
                request.password()));
    }

    public AuthTokens login(LoginRequestDTO request, ClientInfo client) {

        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        if (!(auth.getPrincipal() instanceof UserDetails user)) {
            throw new IllegalStateException("Authenticated principal does not provide user details");
        }

        String refresh = refreshTokenService.startFamily(request.email(), client);
        return new AuthTokens(
                new LoginResponseDTO(
                        user.getUsername(),
                        jwtService.generateAccessToken(user, request.email()),
                        jwtService.getTokenExpire()),
                refresh

        );

    }

    public Optional<AuthTokens> refresh(String rawRefreshToken, ClientInfo client) {
        return refreshTokenService.rotate(rawRefreshToken, client).flatMap(issued -> {
            UserDetails user;
            try {
                user = userDetailsService.loadUserByUsername(issued.subject());
            } catch (UsernameNotFoundException e) {
                refreshTokenService.revokeFamily(issued.familyId());
                return Optional.empty();
            }
            // A disabled/locked user must not be able to keep refreshing
            if (!user.isEnabled() || !user.isAccountNonLocked()) {
                refreshTokenService.revokeFamily(issued.familyId());
                return Optional.empty();
            }
            return Optional.of(new AuthTokens(toResponse(user, issued.subject()), issued.rawToken()));
        });
    }


    private LoginResponseDTO toResponse(UserDetails user, String email) {
        return new LoginResponseDTO(
                user.getUsername(),
                jwtService.generateAccessToken(user, email),
                jwtService.getTokenExpire());
    }
    
    public void logout(String rawRefreshToken) {
        refreshTokenService.revokeFamilyOf(rawRefreshToken);
    }
}
