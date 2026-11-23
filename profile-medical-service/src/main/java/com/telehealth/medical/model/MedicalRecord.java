package com.telehealth.medical.model;

import jakarta.persistence.*;
import kpi.kushnir.privacy.annotations.CryptoShredded;
import kpi.kushnir.privacy.annotations.DataOwner;
import kpi.kushnir.privacy.crypto.PrivacyMeshEntityListener;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "medical_records")
@EntityListeners(PrivacyMeshEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @DataOwner
    @Column(name = "patient_pseudonym", nullable = false, length = 36)
    private String patientPseudonym;

    @Column(name = "doctor_id", nullable = false)
    private UUID doctorId;

    @Column(name = "appointment_id")
    private Long appointmentId;

    @CryptoShredded
    @Column(nullable = false, columnDefinition = "TEXT")
    private String diagnosis;

    @CryptoShredded
    @Column(name = "consultation_notes", columnDefinition = "TEXT")
    private String consultationNotes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
