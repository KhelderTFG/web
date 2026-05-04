package com.khelder.backend.service;

import com.khelder.backend.dto.biometric.BiometricRecordRequest;
import com.khelder.backend.dto.biometric.BiometricRecordResponse;
import com.khelder.backend.entity.BiometricRecord;
import com.khelder.backend.entity.Smartwatch;
import com.khelder.backend.repository.BiometricRecordRepository;
import com.khelder.backend.repository.SmartwatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BiometricRecordService {

    private final BiometricRecordRepository biometricRecordRepository;
    private final SmartwatchRepository      smartwatchRepository;
    private final AlertService              alertService;

    // Umbrales clínicos
    private static final double HR_HIGH_THRESHOLD = 140.0;
    private static final double HR_LOW_THRESHOLD  = 45.0;
    private static final double SPO2_LOW_THRESHOLD = 90.0;

    // -------------------------------------------------------------------------
    // RF-03: Guardar registro biométrico
    // -------------------------------------------------------------------------

    @Transactional
    public BiometricRecordResponse saveRecord(BiometricRecordRequest request) {

        // Verificar que el dispositivo existe
        Smartwatch smartwatch = smartwatchRepository
                .findById(request.getDeviceId())
                .orElseThrow(() -> new IllegalArgumentException(
                    "Dispositivo no encontrado: " + request.getDeviceId()
                ));

        // Convertir timestamp Unix a LocalDateTime
        LocalDateTime timestamp = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(request.getTimestamp()),
                ZoneId.systemDefault()
        );

        // Crear y guardar el registro
        BiometricRecord record = BiometricRecord.builder()
                .deviceId(request.getDeviceId())
                .heartRate(request.getHeartRate())
                .spO2(request.getSpO2())
                .steps(request.getSteps())
                .temperature(request.getTemperature())
                .timestamp(timestamp)
                .build();

        BiometricRecord saved = biometricRecordRepository.save(record);

        // Actualizar último ping del smartwatch
        smartwatchRepository.updatePing(
                request.getDeviceId(),
                LocalDateTime.now(),
                null
        );

        // Evaluar umbrales clínicos y generar alertas si procede
        checkThresholds(request, smartwatch);

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
    // Evaluación de umbrales clínicos
    // -------------------------------------------------------------------------

    private void checkThresholds(BiometricRecordRequest request, Smartwatch smartwatch) {
        if (request.getHeartRate() != null) {
            double hr = request.getHeartRate();
            if (hr > HR_HIGH_THRESHOLD) {
                log.warn("FC elevada detectada: {} bpm en dispositivo {}",
                        hr, request.getDeviceId());
                alertService.createHeartRateAlert(
                        request.getDeviceId(),
                        hr,
                        "HEART_RATE_HIGH",
                        request.getTimestamp()
                );
            } else if (hr < HR_LOW_THRESHOLD) {
                log.warn("FC baja detectada: {} bpm en dispositivo {}",
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
            log.warn("SpO2 baja detectada: {}% en dispositivo {}",
                    request.getSpO2(), request.getDeviceId());
            alertService.createSpO2Alert(
                    request.getDeviceId(),
                    request.getSpO2(),
                    request.getTimestamp()
            );
        }
    }
}