package com.khelder.backend.repository;

import com.khelder.backend.entity.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ReminderRepository extends JpaRepository<Reminder, UUID> {

    // Recordatorios de un paciente — RF-06
    List<Reminder> findByPatientPatientIdOrderByScheduledDateAsc(UUID patientId);

    // Recordatorios pendientes para un dispositivo — consultado por el móvil
    @Query("""
        SELECT r FROM Reminder r
        WHERE r.patient.patientId IN (
            SELECT s.patient.patientId FROM Smartwatch s
            WHERE s.deviceId = :deviceId
        )
        AND r.status = 'PENDING'
        AND r.scheduledDate <= :now
        ORDER BY r.scheduledDate ASC
        """)
    List<Reminder> findPendingByDeviceId(
        @Param("deviceId") String deviceId,
        @Param("now")      LocalDateTime now
    );

    // Recordatorios de un cuidador para un paciente — RF-10
    List<Reminder> findByCaregiverCaregiverIdAndPatientPatientId(
        UUID caregiverId,
        UUID patientId
    );

    // Recordatorios próximos — para alertas preventivas
    @Query("""
        SELECT r FROM Reminder r
        WHERE r.patient.patientId = :patientId
        AND r.status = 'PENDING'
        AND r.scheduledDate BETWEEN :from AND :to
        ORDER BY r.scheduledDate ASC
        """)
    List<Reminder> findUpcoming(
        @Param("patientId") UUID patientId,
        @Param("from")      LocalDateTime from,
        @Param("to")        LocalDateTime to
    );

    List<Reminder> findByCaregiverCaregiverIdOrderByScheduledDateAsc(UUID caregiverId);

    void deleteByPatientPatientId(UUID patientId);

}