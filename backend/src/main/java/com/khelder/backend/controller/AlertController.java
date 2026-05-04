package com.khelder.backend.controller;

import com.khelder.backend.dto.alert.AlertRequest;
import com.khelder.backend.dto.alert.AlertResponse;
import com.khelder.backend.dto.alert.AlertResolveRequest;
import com.khelder.backend.service.AlertService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    /**
     * RF-01, RF-02, RF-13: Recibir alerta desde el móvil
     * POST /api/v1/alerts
     */
    @PostMapping
    public ResponseEntity<AlertResponse> createAlert(
            @Valid @RequestBody AlertRequest request
    ) {
        AlertResponse response = alertService.saveAlert(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * RF-12: Alertas de todos mis pacientes
     * GET /api/v1/alerts?status=ACTIVE&page=0&size=20
     */
    @GetMapping
    public ResponseEntity<Page<AlertResponse>> getMyAlerts(
            @RequestParam(defaultValue = "ACTIVE") String status,
            @RequestParam(defaultValue = "0")      int    page,
            @RequestParam(defaultValue = "20")     int    size
    ) {
        Pageable pageable = PageRequest.of(
                page, size,
                Sort.by("timestamp").descending()
        );
        return ResponseEntity.ok(
                alertService.getMyAlerts(status, pageable)
        );
    }

    /**
     * RF-12: Alertas de un paciente específico
     * GET /api/v1/alerts/patient/{patientId}?status=ACTIVE
     */
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<Page<AlertResponse>> getAlertsByPatient(
            @PathVariable UUID patientId,
            @RequestParam(defaultValue = "ACTIVE") String status,
            @RequestParam(defaultValue = "0")      int    page,
            @RequestParam(defaultValue = "20")     int    size
    ) {
        Pageable pageable = PageRequest.of(
                page, size,
                Sort.by("timestamp").descending()
        );
        return ResponseEntity.ok(
                alertService.getAlertsByPatient(patientId, status, pageable)
        );
    }

    /**
     * RF-12: Resolver una alerta
     * PUT /api/v1/alerts/{alertId}/resolve
     */
    @PutMapping("/{alertId}/resolve")
    public ResponseEntity<AlertResponse> resolveAlert(
            @PathVariable UUID alertId,
            @RequestBody(required = false) AlertResolveRequest request
    ) {
        AlertResolveRequest req = request != null
                ? request
                : new AlertResolveRequest();
        return ResponseEntity.ok(
                alertService.resolveAlert(alertId, req)
        );
    }

    /**
     * RF-12: Cancelar una alerta
     * PUT /api/v1/alerts/{alertId}/cancel
     */
    @PutMapping("/{alertId}/cancel")
    public ResponseEntity<AlertResponse> cancelAlert(
            @PathVariable UUID alertId
    ) {
        return ResponseEntity.ok(alertService.cancelAlert(alertId));
    }
}