package com.telehealth.medical.controller;

import com.telehealth.medical.dto.request.PatientProfileRequest;
import com.telehealth.medical.service.ProfileMedicalService;
import kpi.kushnir.privacy.security.SecurityContextHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/profiles/patient")
@RequiredArgsConstructor
public class PatientProfileController {

    private final ProfileMedicalService profileMedicalService;

    @GetMapping("/me")
    public ResponseEntity<?> getMyPatientProfile() {
        String patientPseudonym = SecurityContextHelper.getCurrentUserPseudonym();
        try {
            return ResponseEntity.ok(profileMedicalService.getPatientProfile(patientPseudonym));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/me")
    public ResponseEntity<?> upsertMyPatientProfile(@RequestBody PatientProfileRequest request) {
        String patientPseudonym = SecurityContextHelper.getCurrentUserPseudonym();
        try {
            return ResponseEntity.ok(profileMedicalService.getOrCreatePatientProfile(
                    patientPseudonym,
                    request.getFirstName(),
                    request.getLastName()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
