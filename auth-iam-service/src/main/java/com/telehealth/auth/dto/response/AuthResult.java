package com.telehealth.auth.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResult {
    private boolean requires2fa;
    private String message;
    private JwtResponse jwtResponse;
}
