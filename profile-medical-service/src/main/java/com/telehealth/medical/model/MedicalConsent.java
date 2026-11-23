package com.telehealth.medical.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "medical_consents",
        uniqueConstraints = @UniqueConstraint(columnNames = {"patient_pseudonym", "doctor_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalConsent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_pseudonym", nullable = false, length = 36)
    private String patientPseudonym;

    @Column(name = "doctor_id", nullable = false)
    private UUID doctorId;

    @Column(nullable = false)
    private boolean granted;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
