package com.telehealth.medical.repository;

import com.telehealth.medical.model.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {
    List<MedicalRecord> findByPatientPseudonymOrderByCreatedAtDesc(String patientPseudonym);
    void deleteByPatientPseudonym(String patientPseudonym);
}
