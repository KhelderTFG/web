package com.khelder.backend.controller;

import com.khelder.backend.dto.smartwatch.PingRequest;
import com.khelder.backend.dto.smartwatch.SmartwatchRequest;
import com.khelder.backend.dto.smartwatch.SmartwatchResponse;
import com.khelder.backend.service.SmartwatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/smartwatches")
@RequiredArgsConstructor
public class SmartwatchController {

    private final SmartwatchService smartwatchService;

    /**
     * RF-15: Registrar smartwatch
     * POST /api/v1/smartwatches
     */
    @PostMapping
    public ResponseEntity<SmartwatchResponse> registerSmartwatch(
            @Valid @RequestBody SmartwatchRequest request
    ) {
        SmartwatchResponse response =
                smartwatchService.registerSmartwatch(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * RF-15: Actualizar ping — llamado por el móvil
     * PUT /api/v1/smartwatches/{deviceId}/ping
     */
    @PutMapping("/{deviceId}/ping")
    public ResponseEntity<SmartwatchResponse> updatePing(
            @PathVariable String deviceId,
            @RequestBody(required = false) PingRequest request
    ) {
        PingRequest req = request != null ? request : new PingRequest();
        return ResponseEntity.ok(smartwatchService.updatePing(deviceId, req));
    }

    /**
     * RF-15: Estado de un smartwatch
     * GET /api/v1/smartwatches/{deviceId}/status
     */
    @GetMapping("/{deviceId}/status")
    public ResponseEntity<SmartwatchResponse> getStatus(
            @PathVariable String deviceId
    ) {
        return ResponseEntity.ok(smartwatchService.getStatus(deviceId));
    }

    /**
     * RF-15: Estado de todos los dispositivos de mis pacientes
     * GET /api/v1/smartwatches
     */
    @GetMapping
    public ResponseEntity<List<SmartwatchResponse>> getMyDevicesStatus() {
        return ResponseEntity.ok(smartwatchService.getMyDevicesStatus());
    }
}