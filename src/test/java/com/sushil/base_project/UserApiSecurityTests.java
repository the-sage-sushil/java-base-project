package com.sushil.base_project;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import com.sushil.base_project.auth.services.RefreshCookieFactory;
import com.sushil.base_project.core.security.JwtService;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@SpringBootTest(properties = "jwt.secret=test-only-secret-key-with-at-least-32-bytes")
@AutoConfigureMockMvc
class UserApiSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Test
    void anonymousCanLoadAngularApp() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<app-root>")));
    }

    @Test
    void anonymousCanAccessPublicHello() throws Exception {
        mockMvc.perform(get("/api/public/hello"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Hello from the public API"));
    }

    @Test
    void anonymousIsUnauthorizedForProfile() throws Exception {
        mockMvc.perform(get("/api/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousIsUnauthorizedForAdminUserList() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserCanAccessProfile() throws Exception {
        mockMvc.perform(get("/api/profile")
                .with(SecurityMockMvcRequestPostProcessors.user("test-user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("test-user"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void userIsForbiddenFromAdminUserList() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void userWithoutDeleteAuthorityIsForbiddenFromDeletingUser() throws Exception {
        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanAccessAdminUserList() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin")
                                .authorities(
                                        new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"),
                                        new org.springframework.security.core.authority.SimpleGrantedAuthority("USER_READ"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void adminWithoutUserReadPermissionIsForbiddenFromAdminUserList() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    void refreshWithoutCookieIsUnauthorizedAndClearsCookie() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Set-Cookie",
                                containsString(RefreshCookieFactory.NAME + "=")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Set-Cookie", containsString("Path=/api/auth")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Set-Cookie", containsString("HttpOnly")));
    }

    @Test
    void authenticatedAccountPasswordEndpointRequiresValidCurrentPassword() throws Exception {
        mockMvc.perform(post("/api/account/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"old-password","newPassword":"NewPassword1!"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void malformedJwtIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/profile")
                        .header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredJwtIsUnauthorized() throws Exception {
        String expired = Jwts.builder()
                .subject("test@example.com")
                .issuedAt(new Date(System.currentTimeMillis() - 60_000))
                .expiration(new Date(System.currentTimeMillis() - 1_000))
                .signWith(Keys.hmacShaKeyFor(
                        "test-only-secret-key-with-at-least-32-bytes".getBytes(StandardCharsets.UTF_8)))
                .compact();

        mockMvc.perform(get("/api/profile")
                        .header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tamperedJwtIsUnauthorized() throws Exception {
        String token = jwtService.generateAccessToken(
                org.springframework.security.core.userdetails.User.withUsername("test-user")
                        .password("unused")
                        .authorities("ROLE_USER")
                        .build(),
                "test@example.com");
        char replacement = token.charAt(token.length() - 1) == 'a' ? 'b' : 'a';
        String tampered = token.substring(0, token.length() - 1) + replacement;

        mockMvc.perform(get("/api/profile")
                        .header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedHeadersArePresentOnApiResponses() throws Exception {
        mockMvc.perform(get("/api/public/hello"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Security-Policy",
                        containsString("frame-ancestors 'none'")))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"));
    }

    @Test
    void actuatorHealthAndOpenApiAreAvailable() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").isNotEmpty());
    }
}
