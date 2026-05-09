package com.khelder.backend.controller;

import com.khelder.backend.dto.auth.ApprovePairingRequest;
import com.khelder.backend.dto.auth.PairingStatusResponse;
import com.khelder.backend.dto.auth.RegisterDeviceRequest;
import com.khelder.backend.entity.Caregiver;
import com.khelder.backend.repository.CaregiverRepository;
import com.khelder.backend.repository.SmartwatchRepository;
import com.khelder.backend.service.PairingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pairing")
@RequiredArgsConstructor
public class PairingController {

    private final PairingService    pairingService;
    private final CaregiverRepository caregiverRepository;
    private final SmartwatchRepository smartwatchRepository;

    /**
     * El móvil registra el código del Watch.
     * No requiere autenticación — el deviceId viene en el header X-Device-Id.
     * POST /api/v1/pairing/register
     */
    @PostMapping("/register")
    public ResponseEntity<Void> register(
            @Valid @RequestBody RegisterDeviceRequest request,
            @RequestHeader("X-Device-Id") String deviceId
    ) {
        pairingService.registerCode(request, deviceId);
        return ResponseEntity.ok().build();
    }

    /**
     * El panel web aprueba el código (cuidador autenticado).
     * POST /api/v1/pairing/approve
     */
    @PostMapping("/approve")
    public ResponseEntity<Void> approve(
            @Valid @RequestBody ApprovePairingRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Caregiver caregiver = caregiverRepository
                .findByEmail(userDetails.getUsername())
                .orElseThrow();
        pairingService.approveCode(request, caregiver.getCaregiverId());
        return ResponseEntity.ok().build();
    }

    /**
     * El móvil hace polling para saber si fue aprobado.
     * No requiere autenticación.
     * GET /api/v1/pairing/status/{code}
     */
    @GetMapping("/status/{code}")
    public ResponseEntity<PairingStatusResponse> status(
            @PathVariable String code
    ) {
        PairingStatusResponse response = pairingService.checkStatus(code);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/fcm-token")
    public ResponseEntity<Void> registerFcmToken(
            @RequestHeader("X-Device-Id") String nodeId,
            @RequestBody Map<String, String> body
    ) {
        String fcmToken = body.get("fcm_token");
        if (fcmToken != null && !fcmToken.isBlank()) {
            smartwatchRepository.updateFcmToken(nodeId, fcmToken);
            // log.info("FCM token registrado para nodeId: {}", nodeId);
        }
        return ResponseEntity.ok().build();
    }
}