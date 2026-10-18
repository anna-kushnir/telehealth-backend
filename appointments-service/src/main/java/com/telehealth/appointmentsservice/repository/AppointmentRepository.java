package com.telehealth.appointmentsservice.repository;

import com.telehealth.appointmentsservice.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByPatientToken(String patientToken);
    List<Appointment> findByTimeSlot_DoctorId(UUID doctorId);
}
