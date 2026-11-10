package com.sushil.base_project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.sushil.base_project.auth.Repository.RefreshTokenRepository;
import com.sushil.base_project.auth.services.RefreshCookieFactory;
import com.sushil.base_project.user.Entities.Role;
import com.sushil.base_project.user.Entities.User;
import com.sushil.base_project.user.Repositories.RoleRepository;
import com.sushil.base_project.user.Repositories.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
class AuthEndpointSecurityTests {

    private static final String EMAIL = "auth-flow@example.com";
    private static final String PASSWORD = "InitialPassword1!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void createAccount() {
        Role role = roleRepository.findByName("ROLE_USER").orElseGet(() -> {
            Role userRole = new Role();
            userRole.setName("ROLE_USER");
            return roleRepository.save(userRole);
        });
        User user = new User();
        user.setUsername("auth-flow-user");
        user.setEmail(EMAIL);
        user.setPassword(passwordEncoder.encode(PASSWORD));
        user.setEnabled(true);
        user.setAccountNotLocked(true);
        user.setRoles(Set.of(role));
        userRepository.save(user);
    }

    @AfterEach
    void cleanUp() {
        refreshTokenRepository.findAll().stream()
                .filter(token -> token.getSubject().equals(EMAIL))
                .forEach(refreshTokenRepository::delete);
        userRepository.findByEmail(EMAIL).ifPresent(userRepository::delete);
        roleRepository.findByName("ROLE_USER").ifPresent(roleRepository::delete);
    }

    @Test
    void loginNormalizesEmailSetsHttpOnlyCookieAndDoesNotReturnRefreshToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"AUTH-FLOW@EXAMPLE.COM","password":"InitialPassword1!"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jwt").isNotEmpty())
                .andReturn();

        String cookie = result.getResponse().getHeader("Set-Cookie");
        assertThat(cookie).contains(RefreshCookieFactory.NAME + "=");
        assertThat(cookie).contains("HttpOnly");
        assertThat(cookie).contains("Path=/api/auth");
        assertThat(result.getResponse().getContentAsString()).doesNotContain(RefreshCookieFactory.NAME);
    }

    @Test
    void wrongPasswordReturnsGenericUnauthorizedAndIncrementsFailureCount() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"AUTH-FLOW@EXAMPLE.COM","password":"incorrect"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));

        assertThat(userRepository.findByEmail(EMAIL).orElseThrow().getFailedLoginAttempts()).isEqualTo(1);
    }

    @Test
    void duplicateEmailRegistrationReturnsConflictAndInvalidInputReturnsFieldErrors() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"another_user","email":"AUTH-FLOW@EXAMPLE.COM","password":"ValidPassword1!"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email is already registered"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"bad","email":"not-an-email","password":"weak"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").isNotEmpty())
                .andExpect(jsonPath("$.errors.password").isNotEmpty());
    }
}
