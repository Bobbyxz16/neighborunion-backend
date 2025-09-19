package com.example.neighborhelp.dto;

import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    public @NotBlank(message = "username is required") String getEmail() {
        return email;
    }

    public @NotBlank(message = "Password is required") String getPassword() {
        return password;
    }
}
