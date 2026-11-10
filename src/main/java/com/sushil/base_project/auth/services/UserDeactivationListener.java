package com.sushil.base_project.auth.services;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.sushil.base_project.auth.services.RefreshTokenService;
import com.sushil.base_project.user.UserDeactivated;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserDeactivationListener {

    private final RefreshTokenService refreshTokenService;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void revokeRefreshTokens(UserDeactivated event) {
        refreshTokenService.revokeAllBySubject(event.email());
    }
}
