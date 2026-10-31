package com.sushil.base_project.auth.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.sushil.base_project.auth.entities.RefreshToken;

import jakarta.persistence.LockModeType;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        Optional<RefreshToken> findByTokenHash(String tokenHash);

        @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Transactional
        @Query("""
                        update RefreshToken token
                        set token.revokedAt = :revokedAt
                        where token.familyId = :familyId and token.revokedAt is null
                        """)
        int revokeAllByFamilyId(@Param("familyId") UUID familyId, @Param("revokedAt") Instant revokedAt);

        @Modifying
        @Transactional
        @Query("""
                        update RefreshToken token
                        set token.revokedAt = :revokedAt
                        where token.subject = :subject and token.revokedAt is null
                        """)
        int revokeAllBySubject(@Param("subject") String subject, @Param("revokedAt") Instant revokedAt);

        @Modifying
        @Query("delete from RefreshToken r where r.expiresAt < :cutoff")
        int deleteExpiredBefore(@Param("cutoff") Instant cutoff);
}
