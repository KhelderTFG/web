package com.khelder.backend.repository;

import com.khelder.backend.entity.Alert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface AlertRepository extends JpaRepository<Alert, UUID> {

    // Alertas por dispositivo ordenadas por timestamp
    List<Alert> findByDeviceIdOrderByTimestampDesc(String deviceId);

    // Alertas activas de todos los pacientes de un cuidador — RF-12
    @Query("""
        SELECT a FROM Alert a
        WHERE a.deviceId IN (
            SELECT s.deviceId FROM Smartwatch s
            WHERE s.patient.patientId IN (
                SELECT cp.patient.patientId FROM CaregiverPatient cp
                WHERE cp.caregiver.caregiverId = :caregiverId
            )
        )
        AND a.status = :status
        ORDER BY a.timestamp DESC
        """)
    Page<Alert> findByCaregiverIdAndStatus(
        @Param("caregiverId") UUID caregiverId,
        @Param("status")      String status,
        Pageable pageable
    );

    // Alertas de un paciente específico con filtro de estado — RF-12
    @Query("""
        SELECT a FROM Alert a
        WHERE a.deviceId IN (
            SELECT s.deviceId FROM Smartwatch s
            WHERE s.patient.patientId = :patientId
        )
        AND (:status IS NULL OR a.status = :status)
        ORDER BY a.timestamp DESC
        """)
    Page<Alert> findByPatientIdAndStatus(
        @Param("patientId") UUID patientId,
        @Param("status")    String status,
        Pageable pageable
    );

    // Contar alertas activas de un cuidador — para el dashboard
    @Query("""
        SELECT COUNT(a) FROM Alert a
        WHERE a.deviceId IN (
            SELECT s.deviceId FROM Smartwatch s
            WHERE s.patient.patientId IN (
                SELECT cp.patient.patientId FROM CaregiverPatient cp
                WHERE cp.caregiver.caregiverId = :caregiverId
            )
        )
        AND a.status = 'ACTIVE'
        """)
    long countActiveByCaregiverId(@Param("caregiverId") UUID caregiverId);

    @Modifying
    @Transactional
    @Query("UPDATE Alert a SET a.status = 'RESOLVED' WHERE a.deviceId = :deviceId AND a.status = 'ACTIVE'")
    void resolveAllByDeviceId(@Param("deviceId") String deviceId);

    void deleteByDeviceId(String deviceId);

}