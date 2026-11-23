package com.telehealth.medical.repository;

import com.telehealth.medical.model.PatientProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PatientProfileRepository extends JpaRepository<PatientProfile, String> {
    Optional<PatientProfile> findByPatientPseudonym(String patientPseudonym);
    void deleteByPatientPseudonym(String patientPseudonym);
}
