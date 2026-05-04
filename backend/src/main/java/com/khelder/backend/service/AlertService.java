package com.khelder.backend.service;

import com.khelder.backend.dto.alert.AlertRequest;
import com.khelder.backend.dto.alert.AlertResponse;
import com.khelder.backend.dto.alert.AlertResolveRequest;
import com.khelder.backend.entity.Alert;
import com.khelder.backend.entity.Patient;
import com.khelder.backend.entity.Smartwatch;
import com.khelder.backend.repository.AlertRepository;
import com.khelder.backend.repository.CaregiverRepository;
import com.khelder.backend.repository.SmartwatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository      alertRepository;
    private final SmartwatchRepository smartwatchRepository;
    private final CaregiverRepository  caregiverRepository;

    // -------------------------------------------------------------------------
    // RF-01, RF-02, RF-13: Recibir alerta desde el móvil
    // -------------------------------------------------------------------------

    @Transactional
    public AlertResponse saveAlert(AlertRequest request) {

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

        return toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // RF-12: Alertas activas de todos los pacientes del cuidador
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<AlertResponse> getMyAlerts(String status, Pageable pageable) {
        UUID caregiverId = getAuthenticatedCaregiverId();

        return alertRepository
                .findByCaregiverIdAndStatus(caregiverId, status, pageable)
                .map(this::toResponse);
    }

    // -------------------------------------------------------------------------
    // RF-12: Alertas de un paciente específico
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<AlertResponse> getAlertsByPatient(
            UUID     patientId,
            String   status,
            Pageable pageable
    ) {
        return alertRepository
                .findByPatientIdAndStatus(patientId, status, pageable)
                .map(this::toResponse);
    }

    // -------------------------------------------------------------------------
    // RF-12: Resolver una alerta
    // -------------------------------------------------------------------------

    @Transactional
    public AlertResponse resolveAlert(
            UUID               alertId,
            AlertResolveRequest request
    ) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Alerta no encontrada: " + alertId
                ));

        if (!alert.getStatus().equals("ACTIVE")) {
            throw new IllegalStateException(
                    "Solo se pueden resolver alertas en estado ACTIVE"
            );
        }

        alert.setStatus("RESOLVED");
        alertRepository.save(alert);

        log.info("Alerta {} resuelta", alertId);

        // TODO Issue #32: notificar al panel web via WebSocket

        return toResponse(alert);
    }

    // -------------------------------------------------------------------------
    // RF-12: Cancelar una alerta
    // -------------------------------------------------------------------------

    @Transactional
    public AlertResponse cancelAlert(UUID alertId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Alerta no encontrada: " + alertId
                ));

        if (!alert.getStatus().equals("ACTIVE")) {
            throw new IllegalStateException(
                    "Solo se pueden cancelar alertas en estado ACTIVE"
            );
        }

        alert.setStatus("CANCELLED");
        alertRepository.save(alert);

        log.info("Alerta {} cancelada", alertId);

        return toResponse(alert);
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
                .heartRate(spO2)
                .timestamp(timestamp)
                .status("ACTIVE")
                .build();

        alertRepository.save(alert);
        log.warn("Alerta SPO2_LOW generada: {}%", spO2);
    }

    // -------------------------------------------------------------------------
    // Mapeo entidad → DTO
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public AlertResponse toResponse(Alert alert) {
        String patientName = null;
        UUID   patientId   = null;

        // Obtener el paciente a través del smartwatch
        Smartwatch smartwatch = smartwatchRepository
                .findById(alert.getDeviceId())
                .orElse(null);

        if (smartwatch != null) {
            Patient patient = smartwatch.getPatient();
            patientName = patient.getFullName();
            patientId   = patient.getPatientId();
        }

        return AlertResponse.builder()
                .alertId(alert.getAlertId())
                .deviceId(alert.getDeviceId())
                .patientName(patientName)
                .patientId(patientId)
                .alertType(alert.getAlertType())
                .status(alert.getStatus())
                .latitude(alert.getLatitude())
                .longitude(alert.getLongitude())
                .heartRate(alert.getHeartRate())
                .batteryLevel(alert.getBatteryLevel())
                .timestamp(alert.getTimestamp())
                .build();
    }

    // -------------------------------------------------------------------------
    // Utilidades
    // -------------------------------------------------------------------------

        private UUID getAuthenticatedCaregiverId() {
                String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
                return caregiverRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException(
                        "Cuidador autenticado no encontrado"
                ))
                .getCaregiverId();
        }
        
        // Añadir en AlertService.java
        @Transactional
        public void createGeofenceAlert(
                String deviceId,
                double latitude,
                double longitude
        ) {
        Alert alert = Alert.builder()
                .deviceId(deviceId)
                .alertType("GEOFENCE_EXIT")
                .latitude(latitude)
                .longitude(longitude)
                .timestamp(LocalDateTime.now())
                .status("ACTIVE")
                .build();

        alertRepository.save(alert);
        log.warn("Alerta GEOFENCE_EXIT generada para dispositivo {}", deviceId);

        // TODO Issue #32: notificar via WebSocket
        }

        // Añadir en AlertService.java
        @Transactional
        public void createBatteryAlert(String deviceId, int batteryLevel) {
        Alert alert = Alert.builder()
                .deviceId(deviceId)
                .alertType("BATTERY_LOW")
                .batteryLevel(batteryLevel)
                .timestamp(LocalDateTime.now())
                .status("ACTIVE")
                .build();

        alertRepository.save(alert);
        log.warn("Alerta BATTERY_LOW generada para dispositivo {}: {}%",
                deviceId, batteryLevel);

        // TODO Issue #32: notificar via WebSocket
        }
}