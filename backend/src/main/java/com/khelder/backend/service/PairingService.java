package com.khelder.backend.service;

import com.khelder.backend.dto.auth.ApprovePairingRequest;
import com.khelder.backend.dto.auth.AuthResponse;
import com.khelder.backend.dto.auth.PairingStatusResponse;
import com.khelder.backend.dto.auth.RegisterDeviceRequest;
import com.khelder.backend.entity.Caregiver;
import com.khelder.backend.entity.PairingCode;
import com.khelder.backend.entity.Patient;
import com.khelder.backend.entity.Smartwatch;
import com.khelder.backend.repository.CaregiverRepository;
import com.khelder.backend.repository.PairingCodeRepository;
import com.khelder.backend.repository.PatientRepository;
import com.khelder.backend.repository.SmartwatchRepository;
import com.khelder.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PairingService {

    private final PairingCodeRepository pairingCodeRepository;
    private final SmartwatchRepository  smartwatchRepository;
    private final CaregiverRepository   caregiverRepository;
    private final PatientRepository     patientRepository;
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

        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Paciente no encontrado: " + request.getPatientId()
                ));

        // Buscar smartwatch existente o crear uno nuevo
        Smartwatch smartwatch = smartwatchRepository
                .findByPatientPatientId(request.getPatientId())
                .orElseGet(() -> {
                        log.info("Creando nuevo smartwatch para paciente: {}", request.getPatientId());
                        return Smartwatch.builder()
                                .deviceId(java.util.UUID.randomUUID().toString())
                                .patient(patient)
                                .connectionStatus(false)
                                .build();
                });

        smartwatch.setNodeId(pairingCode.getDeviceId());
        smartwatchRepository.save(smartwatch);

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
        System.out.println("Buscando código: " + code + " — ahora: " + LocalDateTime.now());

        var approved = pairingCodeRepository
                .findByCodeAndApprovedTrueAndExpiresAtAfter(code, LocalDateTime.now());
        System.out.println("Resultado: " + (approved.isPresent() ? "ENCONTRADO" : "NO ENCONTRADO"));

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


        // Buscar el paciente asociado al smartwatch para incluirlo en la respuesta
        Smartwatch smartwatch = smartwatchRepository.findByNodeId(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("Smartwatch no encontrado: " + deviceId));

        return PairingStatusResponse.builder()
                .status("APPROVED")
                .token(token)
                .deviceId(deviceId)
                .patientId(smartwatch.getPatient().getPatientId().toString())
                .build();
    }
}