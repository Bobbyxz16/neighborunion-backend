package com.example.neighborhelp.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotBlank(message = "Reset token is required")
    private String token;

    @NotBlank(message = "New password is required")
    @Size(min = 6, max = 40, message = "Password must be between 6 and 40 characters")
    private String newPassword;

    @NotBlank(message = "Please confirm your password")
    private String confirmPassword;

    public @NotBlank(message = "Reset token is required") String getToken() {
        return token;
    }

    public @NotBlank(message = "New password is required") @Size(min = 6, max = 40, message = "Password must be between 6 and 40 characters") String getNewPassword() {
        return newPassword;
    }

    public @NotBlank(message = "Please confirm your password") String getConfirmPassword() {
        return confirmPassword;
    }
}