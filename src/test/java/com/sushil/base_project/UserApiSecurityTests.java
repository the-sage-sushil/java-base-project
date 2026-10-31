package com.sushil.base_project;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "jwt.secret=test-only-secret-key-with-at-least-32-bytes")
@AutoConfigureMockMvc
class UserApiSecurityTests {

    @Autowired
    private MockMvc mockMvc;

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
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
