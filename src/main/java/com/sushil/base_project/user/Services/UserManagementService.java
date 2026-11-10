package com.sushil.base_project.user.Services;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.sushil.base_project.core.audit.AuditLogService;
import com.sushil.base_project.core.common.exceptions.DuplicateResourceException;
import com.sushil.base_project.user.RegistrationCommand;
import com.sushil.base_project.user.UserRegistration;
import com.sushil.base_project.user.UserDeactivated;
import com.sushil.base_project.user.Controller.UserApiController.UserSummaryResponse;
import com.sushil.base_project.user.Controller.UserApiController.PermissionResponse;
import com.sushil.base_project.user.Controller.UserApiController.RoleResponse;
import com.sushil.base_project.user.Entities.User;
import com.sushil.base_project.user.Entities.Role;
import com.sushil.base_project.user.Repositories.UserRepository;
import com.sushil.base_project.user.Repositories.RoleRepository;
import com.sushil.base_project.user.Repositories.PermissionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRegistration userRegistration;
    private final AuditLogService auditLog;
    private final ApplicationEventPublisher events;

    public List<UserSummaryResponse> findAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional
    public UserSummaryResponse createUser(
            String username, String email, String password, String ip, String userAgent) {
        String normalizedEmail = normalizeEmail(email);
        userRegistration.register(new RegistrationCommand(username, normalizedEmail, password));
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new IllegalStateException("Created user could not be loaded"));
        auditLog.record("USER_CREATED", normalizedEmail, ip, userAgent, null);
        return toSummary(user);
    }

    @Transactional
    public UserSummaryResponse updateUser(
            long id, String username, String email, String ip, String userAgent) {
        User user = getUser(id);
        String normalizedEmail = normalizeEmail(email);
        if (userRepository.existsByEmailAndIdNot(normalizedEmail, id)) {
            throw new DuplicateResourceException("Email is already registered");
        }
        if (userRepository.existsByUsernameAndIdNot(username, id)) {
            throw new DuplicateResourceException("Username is already taken");
        }
        user.setUsername(username);
        user.setEmail(normalizedEmail);
        auditLog.record("USER_UPDATED", normalizedEmail, ip, userAgent, null);
        return toSummary(user);
    }

    @Transactional
    public UserSummaryResponse setEnabled(
            long id, boolean enabled, String ip, String userAgent) {
        User user = getUser(id);
        user.setEnabled(enabled);
        if (!enabled) {
            events.publishEvent(new UserDeactivated(user.getEmail()));
        } else {
            user.setAccountNotLocked(true);
            user.setFailedLoginAttempts(0);
        }
        auditLog.record("USER_UPDATED", user.getEmail(), ip, userAgent, "enabled=" + enabled);
        return toSummary(user);
    }

    @Transactional
    public UserSummaryResponse assignRoles(
            long id, List<Long> roleIds, String ip, String userAgent) {
        User user = getUser(id);
        Set<Long> uniqueIds = new HashSet<>(roleIds);
        var roles = roleRepository.findAllById(uniqueIds);
        if (roles.size() != uniqueIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more roles do not exist");
        }
        user.setRoles(new HashSet<>(roles));
        auditLog.record(
                "ROLE_ASSIGNED", user.getEmail(), ip, userAgent,
                roles.stream().map(Role::getName).sorted().toList().toString());
        return toSummary(user);
    }

    @Transactional
    public void deleteUser(long id, String ip, String userAgent) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        events.publishEvent(new UserDeactivated(user.getEmail()));
        auditLog.record("USER_DELETED", user.getEmail(), ip, userAgent, null);
        userRepository.delete(user);
    }

    public List<RoleResponse> findAllRoles() {
        return roleRepository.findAll().stream()
                .map(this::toRoleResponse)
                .toList();
    }

    @Transactional
    public RoleResponse createRole(String name, String ip, String userAgent) {
        if (roleRepository.findByName(name).isPresent()) {
            throw new DuplicateResourceException("Role already exists");
        }
        Role role = new Role();
        role.setName(name);
        role.setPermissions(new HashSet<>());
        roleRepository.save(role);
        auditLog.record("ROLE_CREATED", name, ip, userAgent, null);
        return toRoleResponse(role);
    }

    @Transactional
    public RoleResponse assignPermissions(
            long roleId, List<Long> permissionIds, String ip, String userAgent) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));
        Set<Long> uniqueIds = new HashSet<>(permissionIds);
        var permissions = permissionRepository.findAllById(uniqueIds);
        if (permissions.size() != uniqueIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more permissions do not exist");
        }
        role.setPermissions(new HashSet<>(permissions));
        auditLog.record(
                "PERMISSION_CHANGED", role.getName(), ip, userAgent,
                permissions.stream().map(permission -> permission.getName()).sorted().toList().toString());
        return toRoleResponse(role);
    }

    public List<PermissionResponse> findAllPermissions() {
        return permissionRepository.findAll().stream()
                .map(permission -> new PermissionResponse(permission.getId(), permission.getName()))
                .toList();
    }

    private User getUser(long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private UserSummaryResponse toSummary(User user) {
        return new UserSummaryResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.isEnabled(),
                user.getRoles().stream().map(Role::getName).sorted().toList());
    }

    private RoleResponse toRoleResponse(Role role) {
        return new RoleResponse(
                role.getId(),
                role.getName(),
                role.getPermissions().stream()
                        .map(permission -> new PermissionResponse(permission.getId(), permission.getName()))
                        .sorted((first, second) -> first.name().compareTo(second.name()))
                        .toList());
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
