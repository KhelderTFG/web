package com.khelder.backend.controller;

import com.khelder.backend.dto.safezone.SafeZoneRequest;
import com.khelder.backend.dto.safezone.SafeZoneResponse;
import com.khelder.backend.service.SafeZoneService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/safe-zones")
@RequiredArgsConstructor
public class SafeZoneController {

    private final SafeZoneService safeZoneService;

    /**
     * RF-14: Crear zona segura
     * POST /api/v1/safe-zones
     */
    @PostMapping
    public ResponseEntity<SafeZoneResponse> createSafeZone(
            @Valid @RequestBody SafeZoneRequest request
    ) {
        SafeZoneResponse response = safeZoneService.createSafeZone(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * RF-14: Zonas seguras de un paciente
     * GET /api/v1/safe-zones/patient/{patientId}
     */
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<SafeZoneResponse>> getSafeZonesByPatient(
            @PathVariable UUID patientId
    ) {
        return ResponseEntity.ok(
                safeZoneService.getSafeZonesByPatient(patientId)
        );
    }

    /**
     * RF-14: Eliminar zona segura
     * DELETE /api/v1/safe-zones/{zoneId}
     */
    @DeleteMapping("/{zoneId}")
    public ResponseEntity<Void> deleteSafeZone(
            @PathVariable UUID zoneId
    ) {
        safeZoneService.deleteSafeZone(zoneId);
        return ResponseEntity.noContent().build();
    }

    /**
     * RF-14: Verificar geofence manualmente (para testing)
     * GET /api/v1/safe-zones/check?deviceId=x&lat=y&lon=z
     */
    @GetMapping("/check")
    public ResponseEntity<Void> checkGeofence(
            @RequestParam String deviceId,
            @RequestParam double lat,
            @RequestParam double lon
    ) {
        safeZoneService.checkGeofence(deviceId, lat, lon);
        return ResponseEntity.ok().build();
    }
}