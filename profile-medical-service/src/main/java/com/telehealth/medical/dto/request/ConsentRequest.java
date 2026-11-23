package com.telehealth.medical.dto.request;

import lombok.Data;

import java.util.UUID;

@Data
public class ConsentRequest {
    private UUID doctorId;
    private boolean granted;
}
