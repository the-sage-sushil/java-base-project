package com.sushil.base_project.core.security;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class JwtService {

    private final SecretKey secretKey = Keys.hmacShaKeyFor("mySecretKeymySecretKeymySecretKey123".getBytes());

    public String generateAccessToken(UserDetails user) {

        String token = Jwts.builder()
                .subject(user.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 86400000))
                .signWith(secretKey)
                .compact();
        return token;
    }

    public String validateToken(String token, UserDetails user) {

        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String username = claims.getSubject();
        return username;
    }
}
