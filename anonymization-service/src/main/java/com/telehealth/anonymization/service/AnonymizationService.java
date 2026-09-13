package com.telehealth.anonymization.service;

import java.util.UUID;

public interface AnonymizationService {
    String generatePseudonym(UUID patientId);
    UUID resolvePseudonym(String tokenValue);
    void revokePseudonym(String tokenValue);
}
