package com.telehealth.anonymization.service.impl;

import com.telehealth.anonymization.model.PatientNode;
import com.telehealth.anonymization.model.TokenNode;
import com.telehealth.anonymization.repository.PatientNodeRepository;
import com.telehealth.anonymization.repository.TokenNodeRepository;
import com.telehealth.anonymization.service.AnonymizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnonymizationServiceImpl implements AnonymizationService {

    private final PatientNodeRepository patientNodeRepository;
    private final TokenNodeRepository tokenNodeRepository;

    @Override
    @Transactional
    public String generatePseudonym(UUID patientId) {
        String tokenValue = UUID.randomUUID().toString();

        TokenNode tokenNode = TokenNode.builder()
                .tokenValue(tokenValue)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();

        PatientNode patientNode = patientNodeRepository.findById(patientId)
                .orElse(PatientNode.builder().patientId(patientId).build());

        patientNode.getTokens().add(tokenNode);
        patientNodeRepository.save(patientNode);

        return tokenValue;
    }

    @Override
    @Transactional(readOnly = true)
    public UUID resolvePseudonym(String tokenValue) {
        return patientNodeRepository.findPatientIdByValidToken(tokenValue, LocalDateTime.now())
                .orElseThrow(() -> new IllegalArgumentException("Token is invalid, expired, or does not exist. " +
                        "A de-anonymization attempt may have occurred."));
    }

    @Override
    @Transactional
    public void revokePseudonym(String tokenValue) {
        tokenNodeRepository.detachAndDeleteToken(tokenValue);
    }
}
