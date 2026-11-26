package com.auth.mfa.totp.dto.request;

import lombok.Data;

@Data
public class EnrollRequest {
    private String subject;
    private String accountName;
}
