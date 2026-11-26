package com.telehealth.appointmentsservice.service.impl;

import com.telehealth.appointmentsservice.dto.request.CreateAppointmentRequest;
import com.telehealth.appointmentsservice.dto.request.GenerateWeeklyScheduleRequest;
import com.telehealth.appointmentsservice.model.Appointment;
import com.telehealth.appointmentsservice.model.AppointmentStatus;
import com.telehealth.appointmentsservice.model.TimeSlot;
import com.telehealth.appointmentsservice.repository.AppointmentRepository;
import com.telehealth.appointmentsservice.repository.TimeSlotRepository;
import com.telehealth.appointmentsservice.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final RestTemplate restTemplate;

    private void checkDoctorExists(UUID doctorId) {
        String authServiceUrl = "http://localhost:8080/api/v1/auth/doctors/" + doctorId + "/exists";
        try {
            Boolean doctorExists = restTemplate.getForObject(authServiceUrl, Boolean.class);
            if (Boolean.FALSE.equals(doctorExists)) {
                throw new IllegalArgumentException("Doctor with specified ID does not exist or has no ROLE_DOCTOR.");
            }
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new IllegalStateException("Inter-service security error: auth-iam-service blocked the request (401).");
        } catch (RestClientException e) {
            throw new IllegalStateException("Failed to validate doctor via auth-iam-service: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void generateWeeklySchedule(GenerateWeeklyScheduleRequest request) {
        checkDoctorExists(request.getDoctorId());

        LocalDate currentDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();
        if (endDate.isBefore(currentDate)) {
            throw new IllegalArgumentException("End date cannot be before start date.");
        }
        if (request.getWorkingDays() == null) {
            throw new IllegalArgumentException("At least one working day must be chosen.");
        }

        int skippedOverlaps = 0;
        int slotsSaved = 0;

        while (!currentDate.isAfter(endDate)) {
            DayOfWeek dayOfWeek = currentDate.getDayOfWeek();

            if (request.getWorkingDays().containsKey(dayOfWeek)) {
                GenerateWeeklyScheduleRequest.Shift shift = request.getWorkingDays().get(dayOfWeek);

                LocalDateTime currentSlotStart = LocalDateTime.of(currentDate, shift.getStartTime());
                LocalDateTime shiftEnd = LocalDateTime.of(currentDate, shift.getEndTime());

                while (!currentSlotStart.plusMinutes(request.getSlotDurationMinutes()).isAfter(shiftEnd)) {
                    LocalDateTime currentSlotEnd = currentSlotStart.plusMinutes(request.getSlotDurationMinutes());
                    boolean overlaps = timeSlotRepository.existsOverlappingSlot(request.getDoctorId(), currentSlotStart, currentSlotEnd);
                    if (overlaps) {
                        skippedOverlaps++;
                    } else {
                        slotsSaved++;
                        if (!timeSlotRepository.existsByDoctorIdAndStartTime(request.getDoctorId(), currentSlotStart)) {
                            TimeSlot timeSlot = TimeSlot.builder()
                                    .doctorId(request.getDoctorId())
                                    .startTime(currentSlotStart)
                                    .endTime(currentSlotEnd)
                                    .booked(false)
                                    .build();
                            timeSlotRepository.save(timeSlot);
                        }
                    }
                    currentSlotStart = currentSlotEnd;
                }
            }
            currentDate = currentDate.plusDays(1);
        }

        if (slotsSaved == 0 && skippedOverlaps > 0) {
            throw new IllegalArgumentException(
                    "All requested time slots overlap with existing schedule slots for this doctor.");
        }
    }

    @Override
    public List<TimeSlot> getAvailableTimeSlots(UUID doctorId, LocalDateTime start, LocalDateTime end) {
        checkDoctorExists(doctorId);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime effectiveStart = start.isBefore(now) ? now : start;
        if (!effectiveStart.isBefore(end)) {
            return List.of();
        }
        return timeSlotRepository.findByDoctorIdAndBookedFalseAndStartTimeBetweenOrderByStartTimeAsc(
                doctorId, effectiveStart, end);
    }

    @Override
    @Transactional
    public Appointment createAppointment(CreateAppointmentRequest request, String patientPseudonym) {
        TimeSlot timeSlot = timeSlotRepository.findById(request.getSlotId())
                .orElseThrow(() -> new IllegalArgumentException("Time slot not found"));
        checkDoctorExists(timeSlot.getDoctorId());

        if (timeSlot.isBooked()) {
            throw new IllegalStateException("Time slot is already booked");
        }
        if (!timeSlot.getStartTime().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "Cannot book an appointment for a slot that has already started or passed.");
        }

        timeSlot.setBooked(true);
        timeSlotRepository.save(timeSlot);

        Appointment appointment = Appointment.builder()
                .doctorId(timeSlot.getDoctorId())
                .patientToken(patientPseudonym)
                .timeSlot(timeSlot)
                .status(AppointmentStatus.PLANNED)
                .notes(request.getNotes())
                .build();

        return appointmentRepository.save(appointment);
    }

    @Override
    public List<Appointment> getMyAppointments(String patientPseudonym) {
        List<Appointment> appointments = appointmentRepository.findByPatientToken(patientPseudonym);
        autoCancelExpiredAppointments(appointments);
        return appointments;
    }

    @Override
    public List<Appointment> getDoctorAppointments(UUID doctorId) {
        List<Appointment> appointments = appointmentRepository.findByTimeSlot_DoctorId(doctorId);
        autoCancelExpiredAppointments(appointments);
        return appointments;

    }

    private void autoCancelExpiredAppointments(List<Appointment> appointments) {
        LocalDateTime now = LocalDateTime.now();
        List<Appointment> expired = appointments.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.PLANNED)
                .filter(a -> a.getTimeSlot() != null && now.isAfter(a.getTimeSlot().getStartTime().plusMinutes(15)))
                .peek(a -> a.setStatus(AppointmentStatus.EXPIRED))
                .toList();

        if (!expired.isEmpty()) {
            appointmentRepository.saveAll(expired);
        }
    }

    @Override
    @Transactional
    public void completeAppointment(Long appointmentId) {
        var appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + appointmentId));
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointmentRepository.save(appointment);
    }
}
