package com.khelder.backend.service;

import com.khelder.backend.dto.websocket.AlertNotification;
import com.khelder.backend.entity.Alert;
import com.khelder.backend.entity.BiometricRecord;
import com.khelder.backend.entity.Patient;
import com.khelder.backend.entity.Smartwatch;
import com.khelder.backend.repository.CaregiverPatientRepository;
import com.khelder.backend.repository.SmartwatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import com.khelder.backend.entity.BiometricRecord;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketNotificationService {

    private final SimpMessagingTemplate      messagingTemplate;
    private final SmartwatchRepository       smartwatchRepository;
    private final CaregiverPatientRepository caregiverPatientRepository;

    // -------------------------------------------------------------------------
    // Notificar alerta a todos los cuidadores del paciente
    // -------------------------------------------------------------------------

    public void notifyAlert(Alert alert) {
        // Obtener el paciente a través del smartwatch
        Smartwatch smartwatch = smartwatchRepository
                .findById(alert.getDeviceId())
                .orElse(null);

        if (smartwatch == null) {
            log.warn("WebSocket: smartwatch no encontrado para dispositivo {}",
                    alert.getDeviceId());
            return;
        }

        Patient patient = smartwatch.getPatient();

        AlertNotification notification = AlertNotification.builder()
                .alertId(alert.getAlertId())
                .deviceId(alert.getDeviceId())
                .patientId(patient.getPatientId())
                .patientName(patient.getFullName())
                .alertType(alert.getAlertType())
                .status(alert.getStatus())
                .latitude(alert.getLatitude())
                .longitude(alert.getLongitude())
                .heartRate(alert.getHeartRate())
                .batteryLevel(alert.getBatteryLevel())
                .timestamp(alert.getTimestamp())
                .build();

        // Notificar al topic general de alertas
        messagingTemplate.convertAndSend("/topic/alerts", notification);
        log.info("WebSocket: alerta {} enviada al topic /topic/alerts",
                alert.getAlertType());

        // Notificar también al topic específico de cada cuidador del paciente
        caregiverPatientRepository
                .findByIdPatientId(patient.getPatientId())
                .forEach(cp -> {
                    UUID caregiverId = cp.getCaregiver().getCaregiverId();
                    String topic = "/topic/alerts/" + caregiverId;
                    messagingTemplate.convertAndSend(topic, notification);
                    log.debug("WebSocket: alerta enviada al topic {}",  topic);
                });
    }

    // -------------------------------------------------------------------------
    // Notificar cambio de estado de conexión
    // -------------------------------------------------------------------------

    public void notifyConnectionStatus(
            String  deviceId,
            UUID    patientId,
            boolean connected
    ) {
        var payload = java.util.Map.of(
                "device_id",    deviceId,
                "patient_id",   patientId.toString(),
                "connected",    connected,
                "timestamp",    java.time.LocalDateTime.now().toString()
        );

        messagingTemplate.convertAndSend("/topic/connection", payload);
        log.debug("WebSocket: estado de conexión {} → {}",
                deviceId, connected ? "conectado" : "desconectado");
    }

    public void notifyVitals(BiometricRecord record) {
        Smartwatch smartwatch = smartwatchRepository
                .findById(record.getDeviceId())
                .orElse(null);

        if (smartwatch == null) return;

        Patient patient = smartwatch.getPatient();

        var payload = java.util.Map.of(
                "device_id",  record.getDeviceId(),
                "patient_id", patient.getPatientId().toString(),
                "heart_rate", record.getHeartRate() != null ? record.getHeartRate() : 0,
                "spo2",       record.getSpO2() != null ? record.getSpO2() : 0,
                "steps",      record.getSteps() != null ? record.getSteps() : 0,
                "timestamp",  record.getTimestamp().toString()
        );

        caregiverPatientRepository
                .findByIdPatientId(patient.getPatientId())
                .forEach(cp -> {
                        String topic = "/topic/vitals/" + cp.getCaregiver().getCaregiverId();
                        messagingTemplate.convertAndSend(topic, payload);
                });
        }
}