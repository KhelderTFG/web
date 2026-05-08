package com.khelder.backend.repository;

import com.khelder.backend.entity.Smartwatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SmartwatchRepository extends JpaRepository<Smartwatch, String> {

    // Obtener smartwatch por paciente
    Optional<Smartwatch> findByPatientPatientId(UUID patientId);

    // Todos los smartwatches de un paciente (historial de dispositivos)
    List<Smartwatch> findAllByPatientPatientId(UUID patientId);

    Optional<Smartwatch> findByNodeId(String nodeId);

    // Actualizar último ping — RF-15 monitorización de conexión
    @Modifying
    @Transactional
    @Query("""
        UPDATE Smartwatch s
        SET s.lastPing = :lastPing,
            s.connectionStatus = true,
            s.batteryLevel = :batteryLevel
        WHERE s.deviceId = :deviceId
        """)
    int updatePing(
        @Param("deviceId")     String deviceId,
        @Param("lastPing")     LocalDateTime lastPing,
        @Param("batteryLevel") Integer batteryLevel
    );

    // Dispositivos sin ping en los últimos X minutos (desconectados)
    @Query("""
        SELECT s FROM Smartwatch s
        WHERE s.lastPing < :threshold
        OR s.lastPing IS NULL
        """)
    List<Smartwatch> findDisconnected(@Param("threshold") LocalDateTime threshold);
}