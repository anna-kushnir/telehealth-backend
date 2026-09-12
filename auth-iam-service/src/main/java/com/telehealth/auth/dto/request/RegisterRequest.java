package com.telehealth.auth.dto.request;

import com.telehealth.auth.model.RoleName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;

    @NotNull
    private RoleName role;

    private boolean enable2fa;
}
