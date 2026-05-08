package com.khelder.backend.service;

import com.khelder.backend.dto.auth.ApprovePairingRequest;
import com.khelder.backend.dto.auth.AuthResponse;
import com.khelder.backend.dto.auth.PairingStatusResponse;
import com.khelder.backend.dto.auth.RegisterDeviceRequest;
import com.khelder.backend.entity.Caregiver;
import com.khelder.backend.entity.PairingCode;
import com.khelder.backend.entity.Smartwatch;
import com.khelder.backend.repository.CaregiverRepository;
import com.khelder.backend.repository.PairingCodeRepository;
import com.khelder.backend.repository.SmartwatchRepository;
import com.khelder.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PairingService {

    private final PairingCodeRepository pairingCodeRepository;
    private final SmartwatchRepository  smartwatchRepository;
    private final CaregiverRepository   caregiverRepository;
    private final JwtService            jwtService;

    private static final int CODE_TTL_MINUTES = 10;

    // -----------------------------------------------------------------------
    // Llamado por el móvil: registra el código generado por el Watch
    // POST /api/v1/pairing/register
    // -----------------------------------------------------------------------
    @Transactional
    public void registerCode(RegisterDeviceRequest request, String deviceId) {
        // Eliminar código anterior del mismo dispositivo si existe
        pairingCodeRepository.deleteByDeviceId(deviceId);

        PairingCode pairingCode = PairingCode.builder()
                .code(request.getPairingCode())
                .deviceId(deviceId)
                .approved(false)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(CODE_TTL_MINUTES))
                .build();

        pairingCodeRepository.save(pairingCode);
    }

        // -----------------------------------------------------------------------
        // Llamado por el panel web (cuidador loggeado): aprueba el código
        // POST /api/v1/pairing/approve
        // -----------------------------------------------------------------------
        @Transactional
        public void approveCode(ApprovePairingRequest request, UUID caregiverId) {
                PairingCode pairingCode = pairingCodeRepository
                        .findByCodeAndApprovedFalseAndExpiresAtAfter(
                                request.getPairingCode(), LocalDateTime.now()
                        )
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Código inválido o expirado: " + request.getPairingCode()
                        ));

                // Buscar el primer smartwatch del paciente seleccionado y asignarle el nodeId
                smartwatchRepository.findByPatientPatientId(request.getPatientId())
                        .ifPresent(sw -> {
                                sw.setNodeId(pairingCode.getDeviceId());
                                smartwatchRepository.save(sw);
                        });

                pairingCode.setApproved(true);
                pairingCode.setExpiresAt(LocalDateTime.now().plusMinutes(CODE_TTL_MINUTES));
                pairingCodeRepository.save(pairingCode);
        }

    // -----------------------------------------------------------------------
    // Llamado por el móvil en polling: comprueba si el código fue aprobado
    // GET /api/v1/pairing/status/{code}
    // -----------------------------------------------------------------------
    @Transactional
    public PairingStatusResponse checkStatus(String code) {
        var approved = pairingCodeRepository
                .findByCodeAndApprovedTrueAndExpiresAtAfter(code, LocalDateTime.now());

        if (approved.isEmpty()) {
            return PairingStatusResponse.builder()
                    .status("PENDING")
                    .build();
        }

        PairingCode pairingCode = approved.get();
        String deviceId = pairingCode.getDeviceId();

        // Generar JWT para el móvil usando el nodeId del Watch como subject
        UserDetails userDetails = User.builder()
                .username("device:" + deviceId)
                .password("")
                .authorities("ROLE_DEVICE")
                .build();

        String token = jwtService.generateDeviceToken(userDetails);

        // Limpiar el código una vez usado
        pairingCodeRepository.delete(pairingCode);

        return PairingStatusResponse.builder()
                .status("APPROVED")
                .token(token)
                .deviceId(deviceId)
                .build();
    }
}