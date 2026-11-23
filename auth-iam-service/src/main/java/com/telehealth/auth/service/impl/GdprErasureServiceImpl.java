package com.telehealth.auth.service.impl;

import com.telehealth.auth.model.User;
import com.telehealth.auth.repository.UserRepository;
import com.telehealth.auth.service.AuthService;
import com.telehealth.auth.service.GdprErasureService;
import kpi.kushnir.privacy.neo4j.UserIdentityRepository;
import kpi.kushnir.privacy.security.PseudonymizedAuthentication;
import lombok.RequiredArgsConstructor;
//import org.springframework.kafka.core.KafkaTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
public class GdprErasureServiceImpl implements GdprErasureService {

    private static final Logger log = LoggerFactory.getLogger(GdprErasureService.class);
    private static final String GDPR_TOPIC = "gdpr-erasure-events";

    private final UserRepository userRepository;
//    private final KafkaTemplate<String, String> kafkaTemplate;
    private final UserIdentityRepository userIdentityRepository;
    private final JwtDecoder jwtDecoder;

    private final AuthService authService;

    @Override
    @Transactional
    public void executeRightToErasure(String otpCode) {
        log.info("Initiating GDPR Right to Erasure process...");
        try {
            if (otpCode != null && !otpCode.isBlank()) {
                authService.verifyActionOtpForCurrentUser(otpCode);
            }
        } catch (IllegalArgumentException e) {
            log.error("Cannot process erasure: Invalid verification token.");
            throw e;
        }

        String realUserId = extractRealUserId();
        UUID userUuid;
        try {
            userUuid = UUID.fromString(realUserId);
        } catch (IllegalArgumentException e) {
            log.error("Invalid user ID format in token: {}", realUserId);
            throw new IllegalStateException("Cannot process erasure: Invalid User ID format.");
        }

        performCascadeErasure(userUuid);
        log.info("GDPR Right to Erasure process completed successfully fo user {}.", realUserId);
    }

    @Override
    public void executeErasureByAdmin(UUID targetUserId, String adminOtpCode) {
        authService.verifyActionOtpForCurrentUser(adminOtpCode);
        performCascadeErasure(targetUserId);
    }

    private void performCascadeErasure(UUID userUuid) {
        User user = userRepository.findById(userUuid)
                .orElseThrow(() -> new RuntimeException("User not found in IAM database."));
        String realUserId = userUuid.toString();

        log.info("Deleting physical records for user {} from PostgreSQL...", userUuid);
        userRepository.delete(user);

        log.info("Erasing identity graph from Neo4j Privacy Vault for user {}...", realUserId);
        userIdentityRepository.permanentlyEraseUserIdentity(realUserId);

        log.info("Publishing GDPR erasure event to Kafka topic '{}' for user {}...", GDPR_TOPIC, realUserId);
//        kafkaTemplate.send(GDPR_TOPIC, realUserId).whenComplete((result, ex) -> {
//            if (ex = null) {
//                log.info("Successfully published GDPR event for user {}", realUserId);
//            } else {
//                log.error("Failed to publish GDPR event for user {}. Reason: {}", realUserId, ex.getMessage());
//            }
//        });
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
            throw new SecurityException("Original JWT token is missing in security context.");
        }
        return originalToken;
    }
}
