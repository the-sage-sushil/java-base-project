package com.sushil.base_project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import com.sushil.base_project.auth.models.ClientInfo;
import com.sushil.base_project.auth.Repository.RefreshTokenRepository;
import com.sushil.base_project.auth.services.RefreshTokenService;
import com.sushil.base_project.user.Entities.Permission;
import com.sushil.base_project.user.Entities.Role;
import com.sushil.base_project.user.Repositories.PermissionRepository;
import com.sushil.base_project.user.Repositories.RoleRepository;
import com.sushil.base_project.user.Repositories.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
class AdminManagementTests {

    private static final String EMAIL = "admin-target@example.com";
    private static final ClientInfo CLIENT = new ClientInfo("test-agent", "127.0.0.1");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void seedReferenceData() {
        roleRepository.findByName("ROLE_USER").orElseGet(() -> {
            Role role = new Role();
            role.setName("ROLE_USER");
            return roleRepository.save(role);
        });
        if (permissionRepository.findAll().stream().noneMatch(p -> p.getName().equals("USER_READ"))) {
            Permission permission = new Permission();
            permission.setName("USER_READ");
            permissionRepository.save(permission);
        }
    }

    @AfterEach
    void cleanUp() {
        userRepository.findByEmail(EMAIL).ifPresent(userRepository::delete);
        refreshTokenRepository.findAll().stream()
                .filter(token -> token.getSubject().equals(EMAIL))
                .forEach(refreshTokenRepository::delete);
        roleRepository.findByName("ROLE_SUPPORT").ifPresent(roleRepository::delete);
        permissionRepository.findAll().stream()
                .filter(permission -> permission.getName().equals("USER_READ"))
                .forEach(permissionRepository::delete);
    }

    @Test
    void adminCanCreateUserAndDisableRevokesAllRefreshTokens() throws Exception {
        mockMvc.perform(post("/api/admin/users")
                        .with(admin("USER_CREATE", "USER_UPDATE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"target_user","email":"ADMIN-TARGET@example.com","password":"SecurePassword1!"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL));

        Role supportRole = new Role();
        supportRole.setName("ROLE_SUPPORT");
        long supportRoleId = roleRepository.save(supportRole).getId();
        long userId = userRepository.findByEmail(EMAIL).orElseThrow().getId();
        mockMvc.perform(put("/api/admin/users/" + userId + "/roles")
                        .with(admin("USER_UPDATE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleIds\":[" + supportRoleId + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("ROLE_SUPPORT"));

        String refreshToken = refreshTokenService.startFamily(EMAIL, CLIENT);
        mockMvc.perform(patch("/api/admin/users/" + userId + "/enabled")
                        .with(admin("USER_UPDATE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        assertThat(refreshTokenService.rotate(refreshToken, CLIENT)).isEmpty();
    }

    @Test
    void adminCanCreateRoleAndAssignPermissions() throws Exception {
        long permissionId = permissionRepository.findAll().stream()
                .filter(permission -> permission.getName().equals("USER_READ"))
                .findFirst()
                .orElseThrow()
                .getId();
        mockMvc.perform(post("/api/admin/roles")
                        .with(admin("USER_UPDATE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"ROLE_SUPPORT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber());
        long roleId = roleRepository.findByName("ROLE_SUPPORT").orElseThrow().getId();

        mockMvc.perform(put("/api/admin/roles/" + roleId + "/permissions")
                        .with(admin("USER_UPDATE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"permissionIds\":[" + permissionId + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permissions[0].name").value("USER_READ"));
    }

    @Test
    void adminWithoutPermissionCannotCreateUsers() throws Exception {
        mockMvc.perform(post("/api/admin/users")
                        .with(admin("USER_READ"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"target_user","email":"admin-target@example.com","password":"SecurePassword1!"}
                                """))
                .andExpect(status().isForbidden());
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor admin(
            String... permissions) {
        SimpleGrantedAuthority[] authorities = new SimpleGrantedAuthority[permissions.length + 1];
        authorities[0] = new SimpleGrantedAuthority("ROLE_ADMIN");
        for (int index = 0; index < permissions.length; index++) {
            authorities[index + 1] = new SimpleGrantedAuthority(permissions[index]);
        }
        return user("test-admin").authorities(authorities);
    }
}
