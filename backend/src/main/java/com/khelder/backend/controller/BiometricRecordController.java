package com.khelder.backend.controller;

import com.khelder.backend.dto.biometric.BiometricHistoryResponse;
import com.khelder.backend.dto.biometric.BiometricRecordRequest;
import com.khelder.backend.dto.biometric.BiometricRecordResponse;
import com.khelder.backend.service.BiometricRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/biometric-records")
@RequiredArgsConstructor
public class BiometricRecordController {

    private final BiometricRecordService biometricRecordService;

    /**
     * RF-03: Recibir medición del móvil
     * POST /api/v1/biometric-records
     */
    @PostMapping
    public ResponseEntity<BiometricRecordResponse> createRecord(
            @Valid @RequestBody BiometricRecordRequest request
    ) {
        BiometricRecordResponse response =
                biometricRecordService.saveRecord(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * RF-03: Recibir lote de mediciones pendientes
     * POST /api/v1/biometric-records/batch
     */
    @PostMapping("/batch")
    public ResponseEntity<BiometricRecordResponse> createBatch(
            @Valid @RequestBody List<BiometricRecordRequest> requests
    ) {
        BiometricRecordResponse response =
                biometricRecordService.saveBatch(requests);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * RF-11: Historial biométrico de un paciente con paginación
     * GET /api/v1/biometric-records/patient/{patientId}
     *     ?from=2026-01-01T00:00:00
     *     &to=2026-12-31T23:59:59
     *     &page=0
     *     &size=20
     */
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<Page<BiometricHistoryResponse>> getPatientHistory(
            @PathVariable UUID patientId,
            @RequestParam(defaultValue = "#{T(java.time.LocalDateTime).now().minusDays(7)}")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,
            @RequestParam(defaultValue = "#{T(java.time.LocalDateTime).now()}")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(
                page, size,
                Sort.by("timestamp").descending()
        );
        return ResponseEntity.ok(
                biometricRecordService.getPatientHistory(
                        patientId, from, to, pageable
                )
        );
    }

    /**
     * RF-11: Últimas 10 mediciones de un dispositivo
     * GET /api/v1/biometric-records/device/{deviceId}/recent
     */
    @GetMapping("/device/{deviceId}/recent")
    public ResponseEntity<List<BiometricHistoryResponse>> getRecentByDevice(
            @PathVariable String deviceId
    ) {
        return ResponseEntity.ok(
                biometricRecordService.getRecentByDevice(deviceId)
        );
    }

    /**
     * RF-11: Última medición de un dispositivo
     * GET /api/v1/biometric-records/device/{deviceId}/latest
     */
    @GetMapping("/device/{deviceId}/latest")
    public ResponseEntity<BiometricHistoryResponse> getLatestByDevice(
            @PathVariable String deviceId
    ) {
        return ResponseEntity.ok(
                biometricRecordService.getLatestByDevice(deviceId)
        );
    }
}