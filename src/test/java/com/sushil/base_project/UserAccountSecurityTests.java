package com.sushil.base_project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.sushil.base_project.core.common.exceptions.DuplicateResourceException;
import com.sushil.base_project.user.RegistrationCommand;
import com.sushil.base_project.user.UserAccountSecurity;
import com.sushil.base_project.user.Entities.Role;
import com.sushil.base_project.user.Entities.User;
import com.sushil.base_project.user.Repositories.RoleRepository;
import com.sushil.base_project.user.Repositories.UserRepository;
import com.sushil.base_project.user.Services.UserService;

@SpringBootTest
class UserAccountSecurityTests {

    private static final String EMAIL = "account-security@example.com";

    @Autowired
    private UserAccountSecurity userAccountSecurity;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @AfterEach
    void cleanUp() {
        userRepository.findByEmail(EMAIL).ifPresent(userRepository::delete);
        userRepository.findByEmail("mixedcase@example.com").ifPresent(userRepository::delete);
        roleRepository.findByName("ROLE_USER").ifPresent(roleRepository::delete);
    }

    @Test
    void repeatedFailedLoginsLockAccountAndSuccessfulLoginResetsCounter() {
        User user = createUser(EMAIL, true);

        userAccountSecurity.recordFailedLogin(EMAIL, 3);
        userAccountSecurity.recordFailedLogin(EMAIL, 3);
        userAccountSecurity.recordFailedLogin(EMAIL, 3);
        User locked = userRepository.findById(user.getId()).orElseThrow();
        assertThat(locked.getFailedLoginAttempts()).isEqualTo(3);
        assertThat(locked.isAccountNotLocked()).isFalse();

        locked.setAccountNotLocked(true);
        userRepository.save(locked);
        userAccountSecurity.recordSuccessfulLogin(EMAIL);
        assertThat(userRepository.findById(user.getId()).orElseThrow().getFailedLoginAttempts()).isZero();
    }

    @Test
    void passwordChangeValidatesCurrentPasswordAndStoresOnlyAnEncodedPassword() {
        User user = createUser(EMAIL, true);
        userAccountSecurity.changePassword(EMAIL, "InitialPassword1!", "ChangedPassword2!");

        User changed = userRepository.findById(user.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("ChangedPassword2!", changed.getPassword())).isTrue();
        assertThat(changed.getPassword()).isNotEqualTo("ChangedPassword2!");
        assertThatThrownBy(() ->
                userAccountSecurity.changePassword(EMAIL, "wrong-password", "ChangedPassword3!"))
                .isInstanceOf(org.springframework.security.authentication.BadCredentialsException.class);
    }

    @Test
    void registrationNormalizesEmailAndRejectsDuplicateEmailCaseInsensitively() {
        roleRepository.save(Role.builder().name("ROLE_USER").build());
        userService.register(new RegistrationCommand(
                "mixedcase-user", "MixedCase@Example.com", "InitialPassword1!"));

        assertThat(userRepository.findByEmail("mixedcase@example.com")).isPresent();
        assertThatThrownBy(() ->
                userService.register(new RegistrationCommand(
                        "another-user", "MIXEDCASE@example.com", "InitialPassword1!")))
                .isInstanceOf(DuplicateResourceException.class);
    }

    private User createUser(String email, boolean enabled) {
        User user = new User();
        user.setUsername("account-security-user");
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("InitialPassword1!"));
        user.setEnabled(enabled);
        user.setAccountNotLocked(true);
        return userRepository.save(user);
    }
}
