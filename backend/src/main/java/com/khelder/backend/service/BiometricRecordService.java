package com.khelder.backend.service;

import com.khelder.backend.dto.biometric.BiometricHistoryResponse;
import com.khelder.backend.dto.biometric.BiometricRecordRequest;
import com.khelder.backend.dto.biometric.BiometricRecordResponse;
import com.khelder.backend.entity.BiometricRecord;
import com.khelder.backend.entity.Smartwatch;
import com.khelder.backend.repository.BiometricRecordRepository;
import com.khelder.backend.repository.CaregiverRepository;
import com.khelder.backend.repository.PatientRepository;
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
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BiometricRecordService {

    private final BiometricRecordRepository biometricRecordRepository;
    private final SmartwatchRepository      smartwatchRepository;
    private final PatientRepository         patientRepository;
    private final CaregiverRepository       caregiverRepository;
    private final AlertService              alertService;
    private final WebSocketNotificationService webSocketNotificationService;

    // Umbrales clínicos
    private static final double HR_HIGH_THRESHOLD  = 140.0;
    private static final double HR_LOW_THRESHOLD   = 45.0;
    private static final double SPO2_LOW_THRESHOLD = 90.0;

    // -------------------------------------------------------------------------
    // RF-03: Guardar registro biométrico
    // -------------------------------------------------------------------------

    @Transactional
    public BiometricRecordResponse saveRecord(BiometricRecordRequest request) {

        // Verificar que el dispositivo existe
        Smartwatch smartwatch = smartwatchRepository.findById(request.getDeviceId())
        .orElseGet(() -> smartwatchRepository.findByNodeId(request.getDeviceId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Dispositivo no encontrado: " + request.getDeviceId()
                )));

        // Convertir timestamp Unix a LocalDateTime
        LocalDateTime timestamp = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(request.getTimestamp()),
                ZoneId.systemDefault()
        );

        // Crear y guardar el registro
        BiometricRecord record = BiometricRecord.builder()
                .deviceId(smartwatch.getDeviceId())
                .heartRate(request.getHeartRate())
                .spO2(request.getSpO2())
                .steps(request.getSteps())
                .temperature(request.getTemperature())
                .timestamp(timestamp)
                .build();

        BiometricRecord saved = biometricRecordRepository.save(record);
        webSocketNotificationService.notifyVitals(saved);

        // Actualizar último ping del smartwatch
        smartwatchRepository.updatePing(
                smartwatch.getDeviceId(),
                LocalDateTime.now(),
                request.getBatteryLevel()
        );

        // Evaluar umbrales clínicos
        checkThresholds(request);

        log.debug("Registro biométrico guardado: {} para dispositivo {}",
                saved.getRecordId(), request.getDeviceId());

        return BiometricRecordResponse.builder()
                .recordId(saved.getRecordId())
                .status("OK")
                .message("Registro guardado correctamente")
                .build();
    }

    // -------------------------------------------------------------------------
    // RF-03: Guardar lote de registros pendientes de sincronización
    // -------------------------------------------------------------------------

    @Transactional
    public BiometricRecordResponse saveBatch(List<BiometricRecordRequest> requests) {
        requests.forEach(this::saveRecord);
        return BiometricRecordResponse.builder()
                .status("OK")
                .message(requests.size() + " registros guardados")
                .build();
    }

    // -------------------------------------------------------------------------
    // RF-11: Historial biométrico de un paciente con paginación
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<BiometricHistoryResponse> getPatientHistory(
            UUID          patientId,
            LocalDateTime from,
            LocalDateTime to,
            Pageable      pageable
    ) {
        UUID caregiverId = getAuthenticatedCaregiverId();

        if (!patientRepository.existsAssignment(caregiverId, patientId)) {
            throw new SecurityException(
                    "No tienes permisos para ver el historial de este paciente"
            );
        }

        return biometricRecordRepository
                .findByPatientIdAndTimestampBetween(patientId, from, to, pageable)
                .map(this::toHistoryResponse);
    }

    // -------------------------------------------------------------------------
    // RF-11: Última medición de un dispositivo — para el dashboard
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public BiometricHistoryResponse getLatestByDevice(String deviceId) {
        return biometricRecordRepository
                .findFirstByDeviceIdOrderByTimestampDesc(deviceId)
                .map(this::toHistoryResponse)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No hay registros para el dispositivo: " + deviceId
                ));
    }

    // -------------------------------------------------------------------------
    // RF-11: Últimas 10 mediciones de un dispositivo
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<BiometricHistoryResponse> getRecentByDevice(String deviceId) {
        return biometricRecordRepository
                .findTop10ByDeviceIdOrderByTimestampDesc(deviceId)
                .stream()
                .map(this::toHistoryResponse)
                .toList();
    }

    // -------------------------------------------------------------------------
    // Evaluación de umbrales clínicos
    // -------------------------------------------------------------------------

    private void checkThresholds(BiometricRecordRequest request) {

        if (request.getHeartRate() != null) {
            double hr = request.getHeartRate();

            if (hr > HR_HIGH_THRESHOLD) {
                log.warn("FC elevada: {} bpm en dispositivo {}",
                        hr, request.getDeviceId());
                alertService.createHeartRateAlert(
                        request.getDeviceId(),
                        hr,
                        "HEART_RATE_HIGH",
                        request.getTimestamp()
                );
            } else if (hr < HR_LOW_THRESHOLD) {
                log.warn("FC baja: {} bpm en dispositivo {}",
                        hr, request.getDeviceId());
                alertService.createHeartRateAlert(
                        request.getDeviceId(),
                        hr,
                        "HEART_RATE_LOW",
                        request.getTimestamp()
                );
            }
        }

        if (request.getSpO2() != null &&
                request.getSpO2() < SPO2_LOW_THRESHOLD) {
            log.warn("SpO2 baja: {}% en dispositivo {}",
                    request.getSpO2(), request.getDeviceId());
            alertService.createSpO2Alert(
                    request.getDeviceId(),
                    request.getSpO2(),
                    request.getTimestamp()
            );
        }
    }

    // -------------------------------------------------------------------------
    // Mapeo de entidad a DTO
    // -------------------------------------------------------------------------

    private BiometricHistoryResponse toHistoryResponse(BiometricRecord record) {
        return BiometricHistoryResponse.builder()
                .recordId(record.getRecordId())
                .deviceId(record.getDeviceId())
                .heartRate(record.getHeartRate())
                .spO2(record.getSpO2())
                .steps(record.getSteps())
                .temperature(record.getTemperature())
                .timestamp(record.getTimestamp())
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
}