package com.telehealth.medical.repository;

import com.telehealth.medical.model.MedicalConsent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MedicalConsentRepository extends JpaRepository<MedicalConsent, Long> {
    Optional<MedicalConsent> findByPatientPseudonymAndDoctorId(String patientPseudonym, UUID doctorId);
    List<MedicalConsent> findByPatientPseudonym(String patientPseudonym);
    void deleteByPatientPseudonym(String patientPseudonym);
}
