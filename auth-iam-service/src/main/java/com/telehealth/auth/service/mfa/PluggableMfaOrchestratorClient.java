package com.telehealth.auth.service.mfa;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "mfa")
@Data
public class PluggableMfaOrchestratorClient {

    private String internalApiKey;
    private Map<String, ProviderConfig> providers = new HashMap<>();

    private final RestTemplate restTemplate = new RestTemplate();

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-MFA-Api-Key", internalApiKey);
        return headers;
    }

    public String dispatchChallenge(String factorType, String userId, String email) {
        ProviderConfig config = providers.get(factorType);
        if (config == null || !config.isEnabled()) {
            throw new IllegalStateException("MFA factor provider is not enabled: " + factorType);
        }

        String url = config.getBaseUrl() + "/challenge";
        Map<String, String> body = Map.of("subject", userId, "destination", email);
        ResponseEntity<Map> response = restTemplate.postForEntity(
                url, new HttpEntity<>(body, buildHeaders()), Map.class
        );
        return response.getBody() != null ? String.valueOf(response.getBody().get("amr")) : "otp";
    }

    public String verifyFactor(String factorType, String userId, String code) {
        ProviderConfig config = providers.get(factorType);
        if (config == null || !config.isEnabled()) {
            throw new IllegalStateException("MFA factor provider is not enabled: " + factorType);
        }

        String url = config.getBaseUrl() + "/verify";
        Map<String, String> body = Map.of("subject", userId, "code", code);
        ResponseEntity<Map> response = restTemplate.postForEntity(
                url, new HttpEntity<>(body, buildHeaders()), Map.class
        );

        if (response.getBody() == null || !Boolean.TRUE.equals(response.getBody().get("verified"))) {
            throw new IllegalArgumentException("MFA verification failed.");
        }
        return String.valueOf(response.getBody().get("amr"));
    }

    public Map<String, Object> enrollTotp(String userId, String email) {
        ProviderConfig config = providers.get("TOTP");
        if (config == null || !config.isEnabled()) {
            throw new IllegalStateException("TOTP provider is not enabled");
        }

        String url = config.getBaseUrl() + "/enroll";
        Map<String, String> body = Map.of("subject", userId, "accountName", email);
        ResponseEntity<Map> response = restTemplate.postForEntity(
                url, new HttpEntity<>(body, buildHeaders()), Map.class
        );
        return response.getBody();
    }
}
