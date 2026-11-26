package com.telehealth.auth.service.mfa;

import lombok.Data;

@Data
public class ProviderConfig {
    private boolean enabled;
    private String baseUrl;
}
