package com.sushil.base_project.auth.services;

import java.time.Instant;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sushil.base_project.auth.Repository.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;

// auth/services/RefreshTokenCleanup.java
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanup {

    private final RefreshTokenRepository repo;

    @Scheduled(cron = "0 0 3 * * *")        // 03:00 daily, server time (UTC in your setup)
    @Transactional
    public void purgeExpired() {
        repo.deleteExpiredBefore(Instant.now());
    }
}