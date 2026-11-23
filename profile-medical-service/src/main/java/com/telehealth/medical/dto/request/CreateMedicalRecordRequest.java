package com.telehealth.medical.dto.request;

import lombok.Data;

import java.util.UUID;

@Data
public class CreateMedicalRecordRequest {
    private UUID doctorId;
    private String patientPseudonym;
    private Long appointmentId;
    private String diagnosis;
    private String consultationNotes;
}
