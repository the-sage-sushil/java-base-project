package com.sushil.base_project.user.Controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.sushil.base_project.user.Services.CustomUserDetails;
import com.sushil.base_project.user.Services.UserManagementService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class UserApiController {

    private final UserManagementService userManagementService;

    @GetMapping("/api/public/hello")
    public MessageResponse hello() {
        return new MessageResponse("Hello from the public API");
    }

    @GetMapping("/api/profile")
    public ProfileResponse profile(@AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails instanceof CustomUserDetails customUserDetails
                ? customUserDetails.getEmail()
                : null;
        return new ProfileResponse(userDetails.getUsername(), email);
    }

    @GetMapping("/api/admin/users")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('USER_READ')")
    public List<UserSummaryResponse> users() {
        return userManagementService.findAllUsers();
    }

    @PostMapping("/api/admin/users")
    @PreAuthorize("hasAuthority('USER_CREATE')")
    public UserSummaryResponse createUser(
            @Valid @RequestBody CreateUserRequest request, HttpServletRequest http) {
        return userManagementService.createUser(
                request.username(), request.email(), request.password(), http.getRemoteAddr(),
                http.getHeader("User-Agent"));
    }

    @PutMapping("/api/admin/users/{id}")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public UserSummaryResponse updateUser(
            @PathVariable long id, @Valid @RequestBody UpdateUserRequest request, HttpServletRequest http) {
        return userManagementService.updateUser(
                id, request.username(), request.email(), http.getRemoteAddr(), http.getHeader("User-Agent"));
    }

    @PatchMapping("/api/admin/users/{id}/enabled")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public UserSummaryResponse setUserEnabled(
            @PathVariable long id, @Valid @RequestBody EnabledRequest request, HttpServletRequest http) {
        return userManagementService.setEnabled(
                id, request.enabled(), http.getRemoteAddr(), http.getHeader("User-Agent"));
    }

    @PutMapping("/api/admin/users/{id}/roles")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public UserSummaryResponse assignRoles(
            @PathVariable long id,
            @Valid @RequestBody RoleAssignmentRequest request,
            HttpServletRequest http) {
        return userManagementService.assignRoles(
                id, request.roleIds(), http.getRemoteAddr(), http.getHeader("User-Agent"));
    }

    @DeleteMapping({ "/api/admin/users/{id}", "/api/users/{id}" })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('USER_DELETE')")
    public void deleteUser(@PathVariable long id, HttpServletRequest http) {
        userManagementService.deleteUser(id, http.getRemoteAddr(), http.getHeader("User-Agent"));
    }

    @GetMapping("/api/admin/roles")
    @PreAuthorize("hasAuthority('USER_READ')")
    public List<RoleResponse> roles() {
        return userManagementService.findAllRoles();
    }

    @PostMapping("/api/admin/roles")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public RoleResponse createRole(@Valid @RequestBody CreateRoleRequest request, HttpServletRequest http) {
        return userManagementService.createRole(
                request.name(), http.getRemoteAddr(), http.getHeader("User-Agent"));
    }

    @PutMapping("/api/admin/roles/{id}/permissions")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public RoleResponse assignPermissions(
            @PathVariable long id,
            @Valid @RequestBody PermissionAssignmentRequest request,
            HttpServletRequest http) {
        return userManagementService.assignPermissions(
                id, request.permissionIds(), http.getRemoteAddr(), http.getHeader("User-Agent"));
    }

    @GetMapping("/api/admin/permissions")
    @PreAuthorize("hasAuthority('USER_READ')")
    public List<PermissionResponse> permissions() {
        return userManagementService.findAllPermissions();
    }

    public record MessageResponse(String message) {
    }

    public record ProfileResponse(String username, String email) {
    }

    public record UserSummaryResponse(Long id, String username, String email, boolean enabled, List<String> roles) {
    }

    public record CreateUserRequest(
            @NotBlank @Pattern(regexp = "^[a-zA-Z0-9_]{5,50}$") String username,
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Pattern(
                    regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$")
            String password) {
    }

    public record UpdateUserRequest(
            @NotBlank @Pattern(regexp = "^[a-zA-Z0-9_]{5,50}$") String username,
            @NotBlank @Email @Size(max = 255) String email) {
    }

    public record EnabledRequest(@NotNull Boolean enabled) {
    }

    public record CreateRoleRequest(
            @NotBlank @Pattern(regexp = "^ROLE_[A-Z][A-Z0-9_]*$") String name) {
    }

    public record PermissionAssignmentRequest(List<@NotNull @Positive Long> permissionIds) {
        public PermissionAssignmentRequest {
            permissionIds = permissionIds == null ? List.of() : new java.util.ArrayList<>(permissionIds);
        }
    }

    public record RoleAssignmentRequest(List<@NotNull @Positive Long> roleIds) {
        public RoleAssignmentRequest {
            roleIds = roleIds == null ? List.of() : new java.util.ArrayList<>(roleIds);
        }
    }

    public record RoleResponse(Long id, String name, List<PermissionResponse> permissions) {
    }

    public record PermissionResponse(Long id, String name) {
    }
}
