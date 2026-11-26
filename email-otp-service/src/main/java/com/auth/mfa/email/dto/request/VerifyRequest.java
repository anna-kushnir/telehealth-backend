package com.auth.mfa.email.dto.request;

import lombok.Data;

@Data
public class VerifyRequest {
    private String subject;
    private String code;
}
