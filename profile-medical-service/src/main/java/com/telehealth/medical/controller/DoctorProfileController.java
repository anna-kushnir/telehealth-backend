package com.telehealth.medical.controller;

import com.telehealth.medical.model.DoctorProfile;
import com.telehealth.medical.service.ProfileMedicalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profiles/doctors")
@RequiredArgsConstructor
public class DoctorProfileController {

    private final ProfileMedicalService profileMedicalService;

    @GetMapping
    public ResponseEntity<List<DoctorProfile>> getDoctors(@RequestParam(required = false) String specialty) {
        return ResponseEntity.ok(profileMedicalService.getAllDoctors(specialty));
    }

    @GetMapping("/{doctorId}")
    public ResponseEntity<?> getDoctor(@PathVariable UUID doctorId) {
        try {
            return ResponseEntity.ok(profileMedicalService.getDoctorById(doctorId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> upsertDoctor(@RequestBody DoctorProfile doctorProfile) {
        try {
            return ResponseEntity.ok(profileMedicalService.upsertDoctorProfile(doctorProfile));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
