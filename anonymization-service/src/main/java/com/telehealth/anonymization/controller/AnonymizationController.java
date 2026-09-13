package com.telehealth.anonymization.controller;

import com.telehealth.anonymization.service.AnonymizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/anonymization")
@RequiredArgsConstructor
public class AnonymizationController {

    private final AnonymizationService anonymizationService;

    @PostMapping("/generate/{patientId}")
    public ResponseEntity<Map<String, String>> generateToken(@PathVariable UUID patientId) {
        String token = anonymizationService.generatePseudonym(patientId);
        return ResponseEntity.ok(Map.of("pseudonym", token));
    }

    @GetMapping("/resolve/{pseudonym}")
    public ResponseEntity<Map<String, UUID>> resolveToken(@PathVariable String pseudonym) {
        try {
            UUID realId = anonymizationService.resolvePseudonym(pseudonym);
            return ResponseEntity.ok(Map.of("patientId", realId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/revoke/{pseudonym}")
    public ResponseEntity<Void> revokeToken(@PathVariable String pseudonym) {
        anonymizationService.revokePseudonym(pseudonym);
        return ResponseEntity.ok().build();
    }
}
