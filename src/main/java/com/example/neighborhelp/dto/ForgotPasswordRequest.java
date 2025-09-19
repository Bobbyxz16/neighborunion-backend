package com.example.neighborhelp.dto;

import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordRequest {
    @NotBlank(message = "Email is required")
    private String email;

    public @NotBlank(message = "Email is required") String getEmail() {
        return email;
    }
}