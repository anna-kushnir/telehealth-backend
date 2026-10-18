package com.telehealth.appointmentsservice.repository;

import com.telehealth.appointmentsservice.model.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {
    List<TimeSlot> findByDoctorIdAndBookedFalseAndStartTimeBetweenOrderByStartTimeAsc(UUID doctorId, LocalDateTime startTime, LocalDateTime endTime);

    boolean existsByDoctorIdAndStartTime(UUID doctorId, LocalDateTime startTime);

    @Modifying
    @Query("DELETE FROM TimeSlot t WHERE t.doctorId = :doctorId AND t.isBooked = false AND t.startTime >= :start AND t.endTime <= :end")
    void deleteUnbookedSlots(@Param("doctorId") UUID doctorId, @Param("start") LocalDateTime startTime, @Param("end") LocalDateTime end);
}
