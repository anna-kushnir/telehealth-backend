package com.telehealth.appointmentsservice.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;

@Data
public class GenerateWeeklyScheduleRequest {
    @NotNull
    private UUID doctorId;

    @NotNull
    @FutureOrPresent
    private LocalDate startDate;

    @NotNull
    @FutureOrPresent
    private LocalDate endDate;

    @Min(10)
    private int slotDurationMinutes;

    private Map<DayOfWeek, Shift> workingDays;

    @Data
    public static class Shift {
        @NotNull
        private LocalTime startTime;
        @NotNull
        private LocalTime endTime;
    }
}
