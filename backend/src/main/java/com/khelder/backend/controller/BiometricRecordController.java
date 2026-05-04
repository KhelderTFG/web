package com.khelder.backend.controller;

import com.khelder.backend.dto.biometric.BiometricRecordRequest;
import com.khelder.backend.dto.biometric.BiometricRecordResponse;
import com.khelder.backend.service.BiometricRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}