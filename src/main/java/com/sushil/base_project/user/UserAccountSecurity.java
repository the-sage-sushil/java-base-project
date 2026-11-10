package com.sushil.base_project.user;

public interface UserAccountSecurity {

    void recordFailedLogin(String email, int maxAttempts);

    void recordSuccessfulLogin(String email);

    void changePassword(String email, String currentPassword, String newPassword);
}
