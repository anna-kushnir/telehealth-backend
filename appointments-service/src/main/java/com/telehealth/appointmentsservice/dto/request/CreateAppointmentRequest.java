package com.telehealth.appointmentsservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateAppointmentRequest {
    @NotNull
    private Long slotId;

    @NotBlank
    private String patientToken;

    private String notes;
}
