package com.auth.mfa.totp.dto.request;

import lombok.Data;

@Data
public class VerifyRequest {
    private String subject;
    private String code;
}
