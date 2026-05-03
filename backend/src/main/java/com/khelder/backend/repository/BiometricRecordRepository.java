package com.khelder.backend.repository;

import com.khelder.backend.entity.BiometricRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BiometricRecordRepository extends JpaRepository<BiometricRecord, UUID> {

    // Últimos registros de un dispositivo — para el dashboard
    List<BiometricRecord> findTop10ByDeviceIdOrderByTimestampDesc(String deviceId);

    // Último registro de un dispositivo
    Optional<BiometricRecord> findFirstByDeviceIdOrderByTimestampDesc(String deviceId);

    // Historial paginado por dispositivo y rango temporal — RF-11
    Page<BiometricRecord> findByDeviceIdAndTimestampBetween(
        String deviceId,
        LocalDateTime from,
        LocalDateTime to,
        Pageable pageable
    );

    // Historial de un paciente a través de sus smartwatches — RF-11
    @Query("""
        SELECT b FROM BiometricRecord b
        WHERE b.deviceId IN (
            SELECT s.deviceId FROM Smartwatch s
            WHERE s.patient.patientId = :patientId
        )
        AND b.timestamp BETWEEN :from AND :to
        ORDER BY b.timestamp DESC
        """)
    Page<BiometricRecord> findByPatientIdAndTimestampBetween(
        @Param("patientId") UUID patientId,
        @Param("from")      LocalDateTime from,
        @Param("to")        LocalDateTime to,
        Pageable pageable
    );

    // Registros para exportación de informes — RF-16
    @Query("""
        SELECT b FROM BiometricRecord b
        WHERE b.deviceId IN (
            SELECT s.deviceId FROM Smartwatch s
            WHERE s.patient.patientId = :patientId
        )
        AND b.timestamp BETWEEN :from AND :to
        ORDER BY b.timestamp ASC
        """)
    List<BiometricRecord> findAllByPatientIdAndTimestampBetween(
        @Param("patientId") UUID patientId,
        @Param("from")      LocalDateTime from,
        @Param("to")        LocalDateTime to
    );
}