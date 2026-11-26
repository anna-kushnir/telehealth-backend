package com.auth.mfa.email.dto.request;

import lombok.Data;

@Data
public class ChallengeRequest {
    private String subject;
    private String destination;
}
