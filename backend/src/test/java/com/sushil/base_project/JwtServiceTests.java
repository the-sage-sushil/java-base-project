package com.sushil.base_project;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import com.sushil.base_project.core.security.JwtService;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

class JwtServiceTests {

    private static final String SECRET = "test-secret-key-that-is-long-enough-for-hmac-sha-256";

    @Test
    void generatesTokenWithEmailSubjectAndUsernameClaim() {
        JwtService jwtService = new JwtService(SECRET);
        UserDetails user = User.withUsername("notsoshul")
                .password("password")
                .authorities("ROLE_USER")
                .build();

        String token = jwtService.generateAccessToken(user, "user@example.com");
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        var claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();

        assertThat(claims.getSubject()).isEqualTo("user@example.com");
        assertThat(claims.get("email", String.class)).isEqualTo("user@example.com");
        assertThat(claims.get("username", String.class)).isEqualTo("notsoshul");
        assertThat(jwtService.extractUserId(token)).isEqualTo("user@example.com");
        assertThat(jwtService.extractUsername(token)).isEqualTo("notsoshul");
        assertThat(jwtService.validateToken(token, user)).isTrue();
    }
}
