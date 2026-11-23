package com.telehealth.appointmentsservice.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateAppointmentRequest {
    @NotNull
    private Long slotId;

    private String notes;
}
