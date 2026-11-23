package com.telehealth.medical.service.impl;

import com.telehealth.medical.model.DoctorProfile;
import com.telehealth.medical.model.MedicalConsent;
import com.telehealth.medical.model.MedicalRecord;
import com.telehealth.medical.model.PatientProfile;
import com.telehealth.medical.repository.DoctorProfileRepository;
import com.telehealth.medical.repository.MedicalConsentRepository;
import com.telehealth.medical.repository.MedicalRecordRepository;
import com.telehealth.medical.repository.PatientProfileRepository;
import com.telehealth.medical.service.ProfileMedicalService;
import kpi.kushnir.privacy.security.PseudonymResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileMedicalServiceImpl implements ProfileMedicalService {

    private final DoctorProfileRepository doctorProfileRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final MedicalConsentRepository medicalConsentRepository;
    private final PseudonymResolver pseudonymResolver;

    @Value("${spring.application.name}")
    private String currentContextName;

    @Override
    public List<DoctorProfile> getAllDoctors(String specialty) {
        if (specialty != null && !specialty.isBlank()) {
            return doctorProfileRepository.findBySpecialtyIgnoreCase(specialty.trim());
        }
        return doctorProfileRepository.findAll();
    }

    @Override
    public DoctorProfile getDoctorById(UUID doctorId) {
        return doctorProfileRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor profile not found: " + doctorId));
    }

    @Override
    @Transactional
    public DoctorProfile upsertDoctorProfile(DoctorProfile profile) {
        return doctorProfileRepository.findById(profile.getDoctorId())
                .map(existing -> {
                    existing.setFirstName(profile.getFirstName());
                    existing.setLastName(profile.getLastName());
                    existing.setSpecialty(profile.getSpecialty());
                    existing.setPhotoUrl(profile.getPhotoUrl());
                    return doctorProfileRepository.save(existing);
                })
                .orElseGet(() -> doctorProfileRepository.save(profile));
    }

    @Override
    @Transactional
    public PatientProfile getOrCreatePatientProfile(String patientPseudonym, String firstName, String lastName) {
        return patientProfileRepository.findByPatientPseudonym(patientPseudonym)
                .map(existing -> {
                    if (firstName != null && !firstName.isBlank()) {
                        existing.setFirstName(firstName.trim());
                    }
                    if (lastName != null && !lastName.isBlank()) {
                        existing.setLastName(lastName.trim());
                    }
                    return patientProfileRepository.save(existing);
                })
                .orElseGet(() -> patientProfileRepository.save(
                        PatientProfile.builder()
                                .patientPseudonym(patientPseudonym)
                                .firstName(firstName)
                                .lastName(lastName)
                                .build()
                ));
    }

    @Override
    public PatientProfile getPatientProfile(String patientPseudonym) {
        return patientProfileRepository.findByPatientPseudonym(patientPseudonym)
                .orElseThrow(() -> new IllegalArgumentException("Patient profile not found"));
    }

    @Override
    @Transactional
    public MedicalConsent setConsent(String patientPseudonym, UUID doctorId, boolean granted) {
        MedicalConsent consent = medicalConsentRepository.findByPatientPseudonymAndDoctorId(patientPseudonym, doctorId)
                .orElseGet(() -> MedicalConsent.builder()
                        .patientPseudonym(patientPseudonym)
                        .doctorId(doctorId)
                        .build());
        consent.setGranted(granted);
        consent.setUpdatedAt(LocalDateTime.now());
        return medicalConsentRepository.save(consent);
    }

    @Override
    public List<MedicalConsent> getMyConsents(String patientPseudonym) {
        return medicalConsentRepository.findByPatientPseudonym(patientPseudonym);
    }

    @Override
    @Transactional
    public MedicalRecord createMedicalRecord(UUID doctorId, String patientPseudonym, Long appointmentId,
                                             String diagnosis, String consultationNotes) {
        String medicalPseudonym = pseudonymResolver.resolvePeerPseudonym(patientPseudonym, currentContextName);

        MedicalRecord record = MedicalRecord.builder()
                .doctorId(doctorId)
                .patientPseudonym(medicalPseudonym)
                .appointmentId(appointmentId)
                .diagnosis(diagnosis)
                .consultationNotes(consultationNotes)
                .createdAt(LocalDateTime.now())
                .build();
        return medicalRecordRepository.save(record);
    }

    @Override
    public List<MedicalRecord> getPatientMedicalHistoryForDoctor(UUID doctorId, String patientPseudonym) {
        String medicalPseudonym = pseudonymResolver.resolvePeerPseudonym(patientPseudonym, currentContextName);

        boolean hasConsent = medicalConsentRepository
                .findByPatientPseudonymAndDoctorId(medicalPseudonym, doctorId)
                .map(MedicalConsent::isGranted)
                .orElse(false);

        if (!hasConsent) {
            throw new AccessDeniedException("Patient has not granted consent to view medical history");
        }
        return medicalRecordRepository.findByPatientPseudonymOrderByCreatedAtDesc(medicalPseudonym);
    }

    @Override
    public List<MedicalRecord> getOwnMedicalHistory(String patientPseudonym) {
        return medicalRecordRepository.findByPatientPseudonymOrderByCreatedAtDesc(patientPseudonym);
    }
}
