package com.telehealth.appointmentsservice.controller;

import com.telehealth.appointmentsservice.dto.request.CreateAppointmentRequest;
import com.telehealth.appointmentsservice.dto.request.GenerateWeeklyScheduleRequest;
import com.telehealth.appointmentsservice.model.Appointment;
import com.telehealth.appointmentsservice.model.TimeSlot;
import com.telehealth.appointmentsservice.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping("/slots/generate-weekly")
    public ResponseEntity<String> generateWeeklySchedule(@Valid @RequestBody GenerateWeeklyScheduleRequest request) {
        appointmentService.generateWeeklySchedule(request);
        return ResponseEntity.ok("Schedule for specified time period generated successfully");
    }

    @DeleteMapping("/slots/clear")
    public ResponseEntity<String> clearUnbookedSlots(
            @RequestParam UUID doctorId,
            @RequestParam String start,
            @RequestParam String end) {
        appointmentService.clearUnbookedSlots(doctorId, LocalDateTime.parse(start), LocalDateTime.parse(end));
        return ResponseEntity.ok("Unbooked slots deleted successfully");
    }

    @GetMapping("/slots/available")
    public ResponseEntity<List<TimeSlot>> getAvailableSlots(
            @RequestParam UUID doctorId,
            @RequestParam String startDay,
            @RequestParam String endDay) {
        return ResponseEntity.ok(appointmentService.getAvailableTimeSlots(
                doctorId, LocalDateTime.parse(startDay), LocalDateTime.parse(endDay)
        ));
    }

    @PostMapping
    public ResponseEntity<Appointment> bookAppointment(@Valid @RequestBody CreateAppointmentRequest request) {
        return ResponseEntity.ok(appointmentService.createAppointment(request));
    }
}
