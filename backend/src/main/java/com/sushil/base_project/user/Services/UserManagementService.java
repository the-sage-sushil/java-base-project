package com.sushil.base_project.user.Services;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.sushil.base_project.user.Controller.UserApiController.UserSummaryResponse;
import com.sushil.base_project.user.Entities.User;
import com.sushil.base_project.user.Repositories.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final UserRepository userRepository;

    public List<UserSummaryResponse> findAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toSummary)
                .toList();
    }

    public void deleteUser(long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        userRepository.delete(user);
    }

    private UserSummaryResponse toSummary(User user) {
        return new UserSummaryResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.isEnabled());
    }
}
