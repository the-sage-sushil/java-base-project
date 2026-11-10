package com.sushil.base_project.auth.controllers;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sushil.base_project.auth.models.AuthTokens;
import com.sushil.base_project.auth.models.ClientInfo;
import com.sushil.base_project.auth.models.LoginRequestDTO;
import com.sushil.base_project.auth.models.LoginResponseDTO;
import com.sushil.base_project.auth.models.RegisterRequestDTO;
import com.sushil.base_project.auth.services.AuthService;
import com.sushil.base_project.auth.services.RefreshCookieFactory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshCookieFactory cookies;

    @PostMapping("/register")
    public ResponseEntity<Void> createUser(
            @RequestBody @Valid RegisterRequestDTO request, HttpServletRequest http) {
        authService.register(request, ClientInfo.from(http));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid LoginRequestDTO request,
        HttpServletRequest http) {
                
        AuthTokens tokens = authService.login(request, ClientInfo.from(http));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookies.create(tokens.refreshToken()).toString())
                .body(tokens.response());
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            @CookieValue(name = RefreshCookieFactory.NAME, required = false) String refreshToken,
            HttpServletRequest http) {

        return authService.refresh(refreshToken, ClientInfo.from(http))
                .<ResponseEntity<?>>map(t -> ResponseEntity.ok()
                        .header(HttpHeaders.SET_COOKIE, cookies.create(t.refreshToken()).toString())
                        .body(t.response()))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .header(HttpHeaders.SET_COOKIE, cookies.clear().toString())
                        .body(Map.of("message", "Invalid refresh token")));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = RefreshCookieFactory.NAME, required = false) String refreshToken,
            HttpServletRequest http) {
        authService.logout(refreshToken, ClientInfo.from(http));
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookies.clear().toString())
                .build();
    }
}