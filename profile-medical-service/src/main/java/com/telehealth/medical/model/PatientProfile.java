package com.telehealth.medical.model;

import jakarta.persistence.*;
import kpi.kushnir.privacy.annotations.CryptoShredded;
import kpi.kushnir.privacy.crypto.CryptoShreddingConverter;
import lombok.*;

@Entity
@Table(name = "patient_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientProfile {

    @Id
    @Column(name = "patient_pseudonym", length = 36)
    private String patientPseudonym;

    @CryptoShredded
    @Convert(converter = CryptoShreddingConverter.class)
    @Column(nullable = false)
    private String firstName;

    @CryptoShredded
    @Convert(converter = CryptoShreddingConverter.class)
    @Column(nullable = false)
    private String lastName;
}
