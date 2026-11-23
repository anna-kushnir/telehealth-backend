package com.telehealth.medical.controller;

import com.telehealth.medical.dto.request.CreateMedicalRecordRequest;
import com.telehealth.medical.service.ProfileMedicalService;
import kpi.kushnir.privacy.security.SecurityContextHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/medical-records")
@RequiredArgsConstructor
public class MedicalRecordController {

    private final ProfileMedicalService profileMedicalService;

    @GetMapping("/my")
    public ResponseEntity<?> getOwnPatientHistory() {
        String patientPseudonym = SecurityContextHelper.getCurrentUserPseudonym();
        return ResponseEntity.ok(profileMedicalService.getOwnMedicalHistory(patientPseudonym));
    }

    @GetMapping("/patient/{patientPseudonym}")
    public ResponseEntity<?> getHistoryForDoctor(@PathVariable String patientPseudonym, @RequestParam UUID doctorId) {
        try {
            return ResponseEntity.ok(profileMedicalService
                    .getPatientMedicalHistoryForDoctor(doctorId, patientPseudonym));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createMedicalRecord(@RequestBody CreateMedicalRecordRequest request) {
        try {
            return ResponseEntity.ok(profileMedicalService.createMedicalRecord(
                    request.getDoctorId(),
                    request.getPatientPseudonym(),
                    request.getAppointmentId(),
                    request.getDiagnosis(),
                    request.getConsultationNotes()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
