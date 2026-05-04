package com.khelder.backend.controller;

import com.khelder.backend.dto.alert.AlertRequest;
import com.khelder.backend.dto.alert.AlertResponse;
import com.khelder.backend.service.AlertService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<AlertResponse> createAlert(@Valid @RequestBody AlertRequest request) {
        AlertResponse response = alertService.saveAlert(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}