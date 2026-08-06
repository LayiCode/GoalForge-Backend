package com.uthman.VaultApi.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ResetPasswordRequest {

    @NotBlank(message = "Reset token is required")
    private String token;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 64, message = "Password must be at least 8 characters")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^+=_\\-.\\s])[A-Za-z\\d@$!%*?&#^+=_\\-.\\s]{8,64}$",
            message = "Password must include an uppercase letter, a lowercase letter, a number, and a special character"
    )
    private String newPassword;

    public String getToken() { return token; }
    public String getNewPassword() { return newPassword; }
    public void setToken(String token) { this.token = token; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
