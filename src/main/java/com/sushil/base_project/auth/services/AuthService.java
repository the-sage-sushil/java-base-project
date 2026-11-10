package com.sushil.base_project.auth.services;

import java.util.Optional;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.sushil.base_project.core.audit.AuditLogService;
import com.sushil.base_project.auth.models.AuthTokens;
import com.sushil.base_project.auth.models.ClientInfo;
import com.sushil.base_project.auth.models.LoginRequestDTO;
import com.sushil.base_project.auth.models.LoginResponseDTO;
import com.sushil.base_project.auth.models.RegisterRequestDTO;
import com.sushil.base_project.core.security.JwtService;
import com.sushil.base_project.user.RegistrationCommand;
import com.sushil.base_project.user.UserAccountSecurity;
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
    private final UserAccountSecurity userAccountSecurity;
    private final AuditLogService auditLog;

    @Value("${app.security.max-failed-login-attempts:5}")
    private int maxFailedLoginAttempts;

    public void register(RegisterRequestDTO request, ClientInfo client) {
        String email = normalizeEmail(request.email());
        userRegistration.register(new RegistrationCommand(
                request.username(),
                email,
                request.password()));
        auditLog.record("USER_CREATED", email, client.ip(), client.userAgent(), "self_registration");
    }

    public AuthTokens login(LoginRequestDTO request, ClientInfo client) {
        String email = normalizeEmail(request.email());
        Authentication auth;
        try {
            auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password()));
        } catch (AuthenticationException exception) {
            userAccountSecurity.recordFailedLogin(email, maxFailedLoginAttempts);
            auditLog.record("LOGIN_FAILED", email, client.ip(), client.userAgent(), null);
            throw exception;
        }

        if (!(auth.getPrincipal() instanceof UserDetails user)) {
            throw new IllegalStateException("Authenticated principal does not provide user details");
        }

        userAccountSecurity.recordSuccessfulLogin(email);
        auditLog.record("LOGIN_SUCCESS", email, client.ip(), client.userAgent(), null);
        String refresh = refreshTokenService.startFamily(email, client);
        return new AuthTokens(
                new LoginResponseDTO(
                        user.getUsername(),
                        jwtService.generateAccessToken(user, email),
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
    
    public void logout(String rawRefreshToken, ClientInfo client) {
        refreshTokenService.revokeFamilyOf(rawRefreshToken);
        auditLog.record("LOGOUT", null, client.ip(), client.userAgent(), null);
    }

    public void changePassword(
        String email, String currentPassword, String newPassword, ClientInfo client) {
        String normalizedEmail = normalizeEmail(email);
        userAccountSecurity.changePassword(normalizedEmail, currentPassword, newPassword);
        refreshTokenService.revokeAllBySubject(normalizedEmail);
        auditLog.record("PASSWORD_CHANGED", normalizedEmail, client.ip(), client.userAgent(), null);
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
