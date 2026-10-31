package com.sushil.base_project.auth.services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sushil.base_project.auth.Repository.RefreshTokenRepository;
import com.sushil.base_project.auth.entities.RefreshToken;
import com.sushil.base_project.auth.models.ClientInfo;

@Service
public class RefreshTokenService {


    public record Issued(String subject, UUID familyId, String rawToken) {
    }

    private static final int TOKEN_BYTES = 32;
    private static final SecureRandom random = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final Duration ttl;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${app.security.refresh-token-ttl:7d}") Duration ttl) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.ttl = ttl;
    }

    @Transactional
    public String startFamily(String subject, ClientInfo client) {

        return create(subject, UUID.randomUUID(), client);
    }

    @Transactional
    private String create(String subject, UUID randomUUID, ClientInfo client) {

        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        Instant now = Instant.now();
        RefreshToken rt = new RefreshToken();
        rt.setSubject(subject);
        rt.setTokenHash(sha256(raw));
        rt.setFamilyId(randomUUID);
        rt.setCreatedAt(now);
        rt.setExpiresAt(now.plus(ttl));
        rt.setUserAgent(client.userAgent());
        rt.setIp(client.ip());
        refreshTokenRepository.save(rt);

        return raw;
    }

    @Transactional
    public Optional<Issued> rotate(String raw, ClientInfo client) {

        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }

        Instant now = Instant.now();

        RefreshToken rt = refreshTokenRepository.findByTokenHash(sha256(raw)).orElse(null);

        if (rt == null)
            return Optional.empty();

        if (rt.getRevokedAt() != null || !rt.getExpiresAt().isAfter(now)) {
            return Optional.empty();
        }

        if (rt.getUsedAt() != null) {
            // We know that the token is compromised so we delete the whole family
            refreshTokenRepository.revokeAllByFamilyId(rt.getFamilyId(), now);
            return Optional.empty();

        }

        rt.setUsedAt(now);
        String next = create(rt.getSubject(), rt.getFamilyId(), client);
        return Optional.of(new Issued(rt.getSubject(), rt.getFamilyId(), next));

    }

    public void revokeFamily(UUID familyUuid) {

        refreshTokenRepository.revokeAllByFamilyId(familyUuid,Instant.now());

    }

    private static String sha256(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Logout: revoke the whole family the presented token belongs to. */
    @Transactional
    public void revokeFamilyOf(String raw) {
        if (raw == null || raw.isBlank()) return;
        refreshTokenRepository.findByTokenHash(sha256(raw))
            .ifPresent(rt -> refreshTokenRepository.revokeAllByFamilyId(rt.getFamilyId(), Instant.now()));
    }

}
