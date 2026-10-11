package com.sushil.base_project.core.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private final SecretKey key;

    public JwtService(@Value("${jwt.secret}") String secretKeyString) {
        // Build the cryptographic key from your application.properties value
        this.key = Keys.hmacShaKeyFor(secretKeyString.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(UserDetails user, String email) {
        return Jwts.builder()
                .subject(email)
                .claim("email", email)
                .claim("username", user.getUsername())
                .claim("role", user.getAuthorities())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 86400000)) // 24 hours
                .signWith(key)
                .compact();
    }

    public boolean validateToken(String token, UserDetails user) {
        Claims claims = extractAllClaims(token);
        return claims.getSubject().equals(claims.get("email", String.class))
                && user.getUsername().equals(claims.get("username", String.class))
                && !isTokenExpired(token);
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).get("username", String.class);
    }

    // The token subject is the email used to load the user.
    public String extractUserId(String token) {
        return extractAllClaims(token).getSubject();
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }
}