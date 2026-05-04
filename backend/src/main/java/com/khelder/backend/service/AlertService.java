package com.khelder.backend.service;

import com.khelder.backend.dto.alert.AlertRequest;
import com.khelder.backend.dto.alert.AlertResponse;
import com.khelder.backend.entity.Alert;
import com.khelder.backend.repository.AlertRepository;
import com.khelder.backend.repository.SmartwatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository      alertRepository;
    private final SmartwatchRepository smartwatchRepository;

    // -------------------------------------------------------------------------
    // RF-01, RF-02, RF-13: Recibir alerta desde el móvil
    // -------------------------------------------------------------------------

    @Transactional
    public AlertResponse saveAlert(AlertRequest request) {

        // Verificar que el dispositivo existe
        smartwatchRepository.findById(request.getDeviceId())
                .orElseThrow(() -> new IllegalArgumentException(
                    "Dispositivo no encontrado: " + request.getDeviceId()
                ));

        LocalDateTime timestamp = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(request.getTimestamp()),
                ZoneId.systemDefault()
        );

        Alert alert = Alert.builder()
                .deviceId(request.getDeviceId())
                .alertType(request.getAlertType())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .heartRate(request.getHeartRate())
                .batteryLevel(request.getBatteryLevel())
                .timestamp(timestamp)
                .status("ACTIVE")
                .build();

        Alert saved = alertRepository.save(alert);

        log.warn("Alerta {} guardada para dispositivo {}",
                request.getAlertType(), request.getDeviceId());

        // TODO Issue #32: notificar al panel web via WebSocket

        return AlertResponse.builder()
                .alertId(saved.getAlertId())
                .status("OK")
                .message("Alerta registrada correctamente")
                .build();
    }

    // -------------------------------------------------------------------------
    // Alertas generadas internamente por umbrales clínicos
    // -------------------------------------------------------------------------

    @Transactional
    public void createHeartRateAlert(
            String deviceId,
            double heartRate,
            String type,
            long   timestampMs
    ) {
        LocalDateTime timestamp = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestampMs),
                ZoneId.systemDefault()
        );

        Alert alert = Alert.builder()
                .deviceId(deviceId)
                .alertType(type)
                .heartRate(heartRate)
                .timestamp(timestamp)
                .status("ACTIVE")
                .build();

        alertRepository.save(alert);
        log.warn("Alerta {} generada automáticamente: {} bpm", type, heartRate);

        // TODO Issue #32: notificar via WebSocket
    }

    @Transactional
    public void createSpO2Alert(
            String deviceId,
            double spO2,
            long   timestampMs
    ) {
        LocalDateTime timestamp = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestampMs),
                ZoneId.systemDefault()
        );

        Alert alert = Alert.builder()
                .deviceId(deviceId)
                .alertType("SPO2_LOW")
                .heartRate(spO2)  // Reutilizamos el campo para el valor
                .timestamp(timestamp)
                .status("ACTIVE")
                .build();

        alertRepository.save(alert);
        log.warn("Alerta SPO2_LOW generada: {}%", spO2);
    }
}