package com.telehealth.auth.service.impl;

import com.telehealth.auth.model.User;
import com.telehealth.auth.repository.UserRepository;
import com.telehealth.auth.service.GdprErasureService;
import kpi.kushnir.privacy.neo4j.UserIdentityRepository;
import kpi.kushnir.privacy.security.PseudonymizedAuthentication;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public class GdprErasureServiceImpl implements GdprErasureService {

    private static final String GDPR_TOPIC = "gdpr-erasure-events";

    private final UserRepository userRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final UserIdentityRepository userIdentityRepository;
    private final JwtDecoder jwtDecoder;

    @Override
    public void executeRightToErasure() {
//        String pseudonym = SecurityContextHolder.getContext().getAuthentication().getName();
        String realUserId = extractRealUserId();
        UUID userUuid;
        try {
            userUuid = UUID.fromString(realUserId);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Cannot process erasure: Invalid User ID format.");
        }

        User user = userRepository.findById(userUuid)
                .orElseThrow(() -> new RuntimeException("User not found"));

        userRepository.delete(user);
        userIdentityRepository.permanentlyEraseUserIdentity(realUserId);

        kafkaTemplate.send(GDPR_TOPIC, realUserId);
    }

    private String extractRealUserId() {
        String originalToken = getOriginalToken();

        Jwt jwt;
        try {
            jwt = jwtDecoder.decode(originalToken);
        } catch (Exception e) {
            throw new SecurityException("Failed to decode authorization token.");
        }

        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new SecurityException("Token subject is missing.");
        }

        return subject;
    }

    private static String getOriginalToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof PseudonymizedAuthentication)) {
            throw new SecurityException("Invalid authentication type for GDPR operation.");
        }

        String originalToken = (String) authentication.getCredentials();
        if (originalToken == null || originalToken.isBlank()) {
            throw new SecurityException("Original JWT tojen is missing in security context.");
        }
        return originalToken;
    }
}
