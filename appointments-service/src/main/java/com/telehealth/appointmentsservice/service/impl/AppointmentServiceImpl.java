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

    @Override
    @Transactional
    public void generateWeeklySchedule(GenerateWeeklyScheduleRequest request) {
        LocalDate currentDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();

        while (!currentDate.isAfter(endDate)) {
            DayOfWeek dayOfWeek = currentDate.getDayOfWeek();

            if (request.getWorkingDays() != null && request.getWorkingDays().containsKey(dayOfWeek)) {
                GenerateWeeklyScheduleRequest.Shift shift = request.getWorkingDays().get(dayOfWeek);

                LocalDateTime currentSlotStart = LocalDateTime.of(currentDate, shift.getStartTime());
                LocalDateTime shiftEnd = LocalDateTime.of(currentDate, shift.getEndTime());

                while (!currentSlotStart.plusMinutes(request.getSlotDurationMinutes()).isAfter(shiftEnd)) {
                    if (!timeSlotRepository.existsByDoctorIdAndStartTime(request.getDoctorId(), currentSlotStart)) {
                        TimeSlot timeSlot = TimeSlot.builder()
                                .doctorId(request.getDoctorId())
                                .startTime(currentSlotStart)
                                .endTime(currentSlotStart.plusMinutes(request.getSlotDurationMinutes()))
                                .isBooked(false)
                                .build();
                        timeSlotRepository.save(timeSlot);
                    }
                    currentSlotStart = currentSlotStart.plusMinutes(request.getSlotDurationMinutes());
                }
            }
            currentDate = currentDate.plusDays(1);
        }
    }

    @Override
    @Transactional
    public void clearUnbookedSlots(UUID doctorId, LocalDateTime start, LocalDateTime end) {
        timeSlotRepository.deleteUnbookedSlots(doctorId, start, end);
    }

    @Override
    public List<TimeSlot> getAvailableTimeSlots(UUID doctorId, LocalDateTime start, LocalDateTime end) {
        return timeSlotRepository.findByDoctorIdAndBookedFalseAndStartTimeBetweenOrderByStartTimeAsc(
                doctorId, start, end);
    }

    @Override
    @Transactional
    public Appointment createAppointment(CreateAppointmentRequest request) {
        TimeSlot timeSlot = timeSlotRepository.findById(request.getSlotId())
                .orElseThrow(() -> new IllegalArgumentException("Time slot not found"));

        if (timeSlot.isBooked()) {
            throw new IllegalStateException("Time slot is already booked");
        }

        timeSlot.setBooked(true);
        timeSlotRepository.save(timeSlot);

        Appointment appointment = Appointment.builder()
                .patientToken(request.getPatientToken())
                .timeSlot(timeSlot)
                .status(AppointmentStatus.PLANNED)
                .notes(request.getNotes())
                .build();

        return appointmentRepository.save(appointment);
    }
}
