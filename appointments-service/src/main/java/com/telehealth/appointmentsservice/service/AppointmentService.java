package com.telehealth.appointmentsservice.service;

import com.telehealth.appointmentsservice.dto.request.CreateAppointmentRequest;
import com.telehealth.appointmentsservice.dto.request.GenerateWeeklyScheduleRequest;
import com.telehealth.appointmentsservice.model.Appointment;
import com.telehealth.appointmentsservice.model.TimeSlot;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AppointmentService {
    void generateWeeklySchedule(GenerateWeeklyScheduleRequest request);
    void clearUnbookedSlots(UUID doctorId, LocalDateTime start, LocalDateTime end);
    List<TimeSlot> getAvailableTimeSlots(UUID doctorId, LocalDateTime start, LocalDateTime end);
    Appointment createAppointment(CreateAppointmentRequest request);
}
