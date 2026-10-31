package com.sushil.base_project.auth.services;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;


@Component
public class RefreshCookieFactory {

    public static final String NAME = "refresh_token";
    private static final String PATH = "/api/auth";

    private final boolean secure;
    private final Duration ttl;

    public RefreshCookieFactory(@Value("${app.security.cookie-secure:true}") boolean secure,
                                @Value("${app.security.refresh-token-ttl:7d}") Duration ttl) {
        this.secure = secure;
        this.ttl = ttl;
    }

    public ResponseCookie create(String rawToken) {
        return base(rawToken).maxAge(ttl).build();
    }

    public ResponseCookie clear() {
        return base("").maxAge(Duration.ZERO).build();   // same name + path, or the browser won't overwrite it
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)          // JS can't read it, so XSS can't steal it
                .secure(secure)
                .sameSite("Strict")
                .path(PATH);             // browser only sends it to /api/auth/**
    }
}