package com.telehealth.appointmentsservice.controller;

import com.telehealth.appointmentsservice.dto.request.CreateAppointmentRequest;
import com.telehealth.appointmentsservice.dto.request.GenerateWeeklyScheduleRequest;
import com.telehealth.appointmentsservice.model.Appointment;
import com.telehealth.appointmentsservice.service.AppointmentService;
import jakarta.validation.Valid;
import kpi.kushnir.privacy.security.SecurityContextHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @GetMapping("/slots/available")
    public ResponseEntity<?> getAvailableSlots(
            @RequestParam UUID doctorId,
            @RequestParam String start,
            @RequestParam String end) {
        try {
            return ResponseEntity.ok(appointmentService.getAvailableTimeSlots(
                    doctorId, LocalDateTime.parse(start), LocalDateTime.parse(end)
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/slots/generate-weekly")
    public ResponseEntity<?> generateWeeklySchedule(@Valid @RequestBody GenerateWeeklyScheduleRequest request) {
        try {
            // TODO: doctor can generate schedule only for himself
            appointmentService.generateWeeklySchedule(request);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
        return ResponseEntity.ok("Schedule for specified time period generated successfully");
    }

    // TODO: delete this method or call it from the frontend
    @DeleteMapping("/slots/clear")
    public ResponseEntity<?> clearUnbookedSlots(
            @RequestParam UUID doctorId,
            @RequestParam String start,
            @RequestParam String end) {
        try {
            appointmentService.clearUnbookedSlots(doctorId, LocalDateTime.parse(start), LocalDateTime.parse(end));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
        return ResponseEntity.ok("Unbooked slots deleted successfully");
    }

    @GetMapping("/my")
    public ResponseEntity<List<Appointment>> getMyAppointments() {
        String patientPseudonym = SecurityContextHelper.getCurrentUserPseudonym();
        // TODO: when doctor is deleted - smth must be done with his appointments
        return ResponseEntity.ok(appointmentService.getMyAppointments(patientPseudonym));
    }

    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<List<Appointment>> getDoctorAppointments(@PathVariable UUID doctorId) {
        return ResponseEntity.ok(appointmentService.getDoctorAppointments(doctorId));
    }

    @PostMapping
    public ResponseEntity<?> bookAppointment(@Valid @RequestBody CreateAppointmentRequest request) {
        String patientPseudonym = SecurityContextHelper.getCurrentUserPseudonym();
        try {
            return ResponseEntity.ok(appointmentService.createAppointment(request, patientPseudonym));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
