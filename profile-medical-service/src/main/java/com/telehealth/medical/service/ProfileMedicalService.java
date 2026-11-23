package com.telehealth.medical.service;

import com.telehealth.medical.model.DoctorProfile;
import com.telehealth.medical.model.MedicalConsent;
import com.telehealth.medical.model.MedicalRecord;
import com.telehealth.medical.model.PatientProfile;

import java.util.List;
import java.util.UUID;

public interface ProfileMedicalService {
    List<DoctorProfile> getAllDoctors(String specialty);
    DoctorProfile getDoctorById(UUID doctorId);
    DoctorProfile upsertDoctorProfile(DoctorProfile profile);

    PatientProfile getOrCreatePatientProfile(String patientPseudonym, String firstName, String lastName);
    PatientProfile getPatientProfile(String patientPseudonym);

    MedicalConsent setConsent(String patientPseudonym, UUID doctorId, boolean granted);
    List<MedicalConsent> getMyConsents(String patientPseudonym);

    MedicalRecord createMedicalRecord(UUID doctorId, String patientPseudonym, Long appointmentId, String diagnosis, String consultationNotes);
    List<MedicalRecord> getPatientMedicalHistoryForDoctor(UUID doctorId, String patientPseudonym);
    List<MedicalRecord> getOwnMedicalHistory(String patientPseudonym);
}
