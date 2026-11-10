package com.sushil.base_project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.sushil.base_project.auth.Repository.RefreshTokenRepository;
import com.sushil.base_project.auth.entities.RefreshToken;
import com.sushil.base_project.auth.models.ClientInfo;
import com.sushil.base_project.auth.services.AuthService;
import com.sushil.base_project.auth.services.RefreshCookieFactory;
import com.sushil.base_project.auth.services.RefreshTokenService;
import com.sushil.base_project.user.Entities.User;
import com.sushil.base_project.user.Repositories.UserRepository;

import jakarta.servlet.http.Cookie;

@SpringBootTest
@AutoConfigureMockMvc
class RefreshTokenLifecycleTests {

    private static final ClientInfo CLIENT = new ClientInfo("test-agent", "127.0.0.1");

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private MockMvc mockMvc;

    @AfterEach
    void cleanUp() {
        refreshTokenRepository.deleteAll();
        userRepository.findByEmail("disabled-refresh@example.com").ifPresent(userRepository::delete);
    }

    @Test
    void validTokenRotatesAndUsedTokenReplayRevokesItsEntireFamily() {
        String original = refreshTokenService.startFamily("user@example.com", CLIENT);
        RefreshTokenService.Issued latest = refreshTokenService.rotate(original, CLIENT).orElseThrow();

        assertThat(refreshTokenService.rotate(original, CLIENT)).isEmpty();
        assertThat(refreshTokenService.rotate(latest.rawToken(), CLIENT)).isEmpty();
        assertThat(refreshTokenRepository.findAll())
                .allSatisfy(token -> assertThat(token.getRevokedAt()).isNotNull());
    }

    @Test
    void expiredAndRevokedTokensCannotRotate() {
        String expired = refreshTokenService.startFamily("expired@example.com", CLIENT);
        RefreshToken expiredRow = refreshTokenRepository.findAll().get(0);
        expiredRow.setExpiresAt(Instant.now().minusSeconds(1));
        refreshTokenRepository.save(expiredRow);
        assertThat(refreshTokenService.rotate(expired, CLIENT)).isEmpty();

        String revoked = refreshTokenService.startFamily("revoked@example.com", CLIENT);
        RefreshToken revokedRow = refreshTokenRepository.findAll().stream()
                .filter(token -> token.getSubject().equals("revoked@example.com"))
                .findFirst()
                .orElseThrow();
        refreshTokenService.revokeFamily(revokedRow.getFamilyId());
        assertThat(refreshTokenService.rotate(revoked, CLIENT)).isEmpty();
    }

    @Test
    void concurrentRotationSucceedsOnceAndReplayRevokesTheFamily() throws Exception {
        String original = refreshTokenService.startFamily("concurrent@example.com", CLIENT);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> rotateAfter(start, original));
            var second = executor.submit(() -> rotateAfter(start, original));
            start.countDown();

            Optional<RefreshTokenService.Issued> firstResult = first.get(10, TimeUnit.SECONDS);
            Optional<RefreshTokenService.Issued> secondResult = second.get(10, TimeUnit.SECONDS);

            assertThat((firstResult.isPresent() ? 1 : 0) + (secondResult.isPresent() ? 1 : 0)).isEqualTo(1);
            RefreshToken latest = refreshTokenRepository.findAll().stream()
                    .filter(token -> token.getSubject().equals("concurrent@example.com"))
                    .filter(token -> token.getUsedAt() == null)
                    .findFirst()
                    .orElseThrow();
            assertThat(latest.getRevokedAt()).isNotNull();
        }
    }

    @Test
    void refreshWithoutCookieReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutInvalidatesPresentedCookie() throws Exception {
        String rawToken = refreshTokenService.startFamily("logout@example.com", CLIENT);
        Cookie cookie = new Cookie(RefreshCookieFactory.NAME, rawToken);

        mockMvc.perform(post("/api/auth/logout").cookie(cookie))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/auth/refresh").cookie(cookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void disabledUserCannotRefresh() {
        User user = createUser("disabled-user", "disabled-refresh@example.com", false);

        String original = refreshTokenService.startFamily(user.getEmail(), CLIENT);
        RefreshTokenService.Issued rotated = refreshTokenService.rotate(original, CLIENT).orElseThrow();

        assertThat(authService.refresh(rotated.rawToken(), CLIENT)).isEmpty();
        assertThat(refreshTokenRepository.findAll())
                .allSatisfy(token -> assertThat(token.getRevokedAt()).isNotNull());
    }

    @Test
    void refreshEndpointRotatesCookieAndReturnsAccessToken() throws Exception {
        User user = createUser("http-refresh-user", "http-refresh@example.com", true);
        String rawToken = refreshTokenService.startFamily(user.getEmail(), CLIENT);

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie(RefreshCookieFactory.NAME, rawToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jwt").isNotEmpty())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Set-Cookie", org.hamcrest.Matchers.containsString("HttpOnly")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Path=/api/auth")));
    }

    private Optional<RefreshTokenService.Issued> rotateAfter(CountDownLatch start, String token)
            throws InterruptedException {
        start.await();
        return refreshTokenService.rotate(token, CLIENT);
    }

    private User createUser(String username, String email, boolean enabled) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("not-a-real-password");
        user.setEnabled(enabled);
        user.setAccountNotLocked(true);
        return userRepository.save(user);
    }
}
