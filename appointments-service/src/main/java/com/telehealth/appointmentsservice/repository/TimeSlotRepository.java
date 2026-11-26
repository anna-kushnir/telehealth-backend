package com.telehealth.appointmentsservice.repository;

import com.telehealth.appointmentsservice.model.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
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

    @Query("SELECT COUNT(t) > 0 FROM TimeSlot t WHERE t.doctorId = :doctorId " +
            "AND t.startTime < :newEnd AND t.endTime > :newStart")
    boolean existsOverlappingSlot(@Param("doctorId") UUID doctorId,
                                  @Param("newStart") LocalDateTime newStart,
                                  @Param("newEnd") LocalDateTime newEnd);
}
