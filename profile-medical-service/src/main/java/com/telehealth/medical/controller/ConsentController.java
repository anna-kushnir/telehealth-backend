package com.telehealth.medical.controller;

import com.telehealth.medical.dto.request.ConsentRequest;
import com.telehealth.medical.service.ProfileMedicalService;
import kpi.kushnir.privacy.security.SecurityContextHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/consents")
@RequiredArgsConstructor
public class ConsentController {

    private final ProfileMedicalService profileMedicalService;

    @GetMapping("/my")
    public ResponseEntity<?> getMyConsents() {
        String patientPseudonym = SecurityContextHelper.getCurrentUserPseudonym();
        return ResponseEntity.ok(profileMedicalService.getMyConsents(patientPseudonym));
    }

    @PostMapping
    public ResponseEntity<?> setConsent(@RequestBody ConsentRequest request) {
        String patientPseudonym = SecurityContextHelper.getCurrentUserPseudonym();
        try {
            return ResponseEntity.ok(profileMedicalService.setConsent(
                    patientPseudonym, request.getDoctorId(),
                    request.isGranted()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
