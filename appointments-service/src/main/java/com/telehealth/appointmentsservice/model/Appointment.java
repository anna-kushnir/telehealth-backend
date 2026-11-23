package com.telehealth.appointmentsservice.model;

import jakarta.persistence.*;
import kpi.kushnir.privacy.annotations.CryptoShredded;
import kpi.kushnir.privacy.annotations.DataOwner;
import kpi.kushnir.privacy.crypto.PrivacyMeshEntityListener;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "appointments")
@EntityListeners(PrivacyMeshEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "doctor_id", nullable = false)
    private UUID doctorId;

    @DataOwner
    @Column(name = "patient_token", nullable = false, length = 36)
    private String patientToken;

    @OneToOne
    @JoinColumn(name = "slot_id", nullable = false, unique = true)
    private TimeSlot timeSlot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppointmentStatus status;

    @CryptoShredded
    @Column(length = 500)
    private String notes;
}
