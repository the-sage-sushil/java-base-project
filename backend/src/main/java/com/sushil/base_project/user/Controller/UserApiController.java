package com.sushil.base_project.user.Controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.sushil.base_project.user.Services.CustomUserDetails;
import com.sushil.base_project.user.Services.UserManagementService;

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
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public List<UserSummaryResponse> users() {
        return userManagementService.findAllUsers();
    }

    @DeleteMapping("/api/users/{id}")
    @PreAuthorize("hasAuthority('USER_DELETE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable long id) {
        userManagementService.deleteUser(id);
    }

    public record MessageResponse(String message) {
    }

    public record ProfileResponse(String username, String email) {
    }

    public record UserSummaryResponse(Long id, String username, String email, boolean enabled) {
    }
}
