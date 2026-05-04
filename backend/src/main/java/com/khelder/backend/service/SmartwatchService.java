package com.khelder.backend.service;

import com.khelder.backend.dto.smartwatch.PingRequest;
import com.khelder.backend.dto.smartwatch.SmartwatchRequest;
import com.khelder.backend.dto.smartwatch.SmartwatchResponse;
import com.khelder.backend.entity.GpsLocation;
import com.khelder.backend.entity.Patient;
import com.khelder.backend.entity.Smartwatch;
import com.khelder.backend.repository.CaregiverRepository;
import com.khelder.backend.repository.GpsLocationRepository;
import com.khelder.backend.repository.PatientRepository;
import com.khelder.backend.repository.SmartwatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmartwatchService {

    private final SmartwatchRepository  smartwatchRepository;
    private final PatientRepository     patientRepository;
    private final CaregiverRepository   caregiverRepository;
    private final GpsLocationRepository gpsLocationRepository;
    private final SafeZoneService       safeZoneService;
    private final AlertService          alertService;
    private final WebSocketNotificationService webSocketNotificationService;

    // Tiempo máximo sin ping antes de considerar desconectado
    private static final int DISCONNECT_THRESHOLD_MINUTES = 10;

    // Umbral de batería crítica
    private static final int BATTERY_CRITICAL_THRESHOLD = 15;

    private final GeometryFactory geometryFactory =
            new GeometryFactory(new PrecisionModel(), 4326);

    // -------------------------------------------------------------------------
    // RF-15: Registrar nuevo smartwatch
    // -------------------------------------------------------------------------

    @Transactional
    public SmartwatchResponse registerSmartwatch(SmartwatchRequest request) {
        UUID caregiverId = getAuthenticatedCaregiverId();

        if (!patientRepository.existsAssignment(caregiverId, request.getPatientId())) {
            throw new SecurityException(
                "No tienes permisos para registrar dispositivos para este paciente"
            );
        }

        Patient patient = patientRepository
                .findById(request.getPatientId())
                .orElseThrow(() -> new IllegalArgumentException(
                    "Paciente no encontrado: " + request.getPatientId()
                ));

        // Si ya existe el dispositivo lo actualizamos
        Smartwatch smartwatch = smartwatchRepository
                .findById(request.getDeviceId())
                .orElse(Smartwatch.builder()
                        .deviceId(request.getDeviceId())
                        .build());

        smartwatch.setPatient(patient);
        smartwatch.setConnectionStatus(false);

        Smartwatch saved = smartwatchRepository.save(smartwatch);

        log.info("Smartwatch registrado: {} para paciente {}",
                saved.getDeviceId(), request.getPatientId());

        return toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // RF-15: Actualizar ping — llamado por el móvil periódicamente
    // -------------------------------------------------------------------------

    @Transactional
    public SmartwatchResponse updatePing(String deviceId, PingRequest request) {

        Smartwatch smartwatch = smartwatchRepository
                .findById(deviceId)
                .orElseThrow(() -> new IllegalArgumentException(
                    "Dispositivo no encontrado: " + deviceId
                ));

        LocalDateTime now = LocalDateTime.now();

        smartwatchRepository.updatePing(deviceId, now, request.getBatteryLevel());

        // Actualizar estado en memoria para la respuesta
        smartwatch.setLastPing(now);
        smartwatch.setConnectionStatus(true);
        if (request.getBatteryLevel() != null) {
            smartwatch.setBatteryLevel(request.getBatteryLevel());
        }

        // Comprobar batería crítica — RF-13
        if (request.getBatteryLevel() != null &&
                request.getBatteryLevel() < BATTERY_CRITICAL_THRESHOLD) {
            log.warn("Batería crítica en dispositivo {}: {}%",
                    deviceId, request.getBatteryLevel());
            alertService.createBatteryAlert(deviceId, request.getBatteryLevel());
        }

        // Guardar ubicación GPS y verificar geofence — RF-14
        if (request.getLatitude() != null && request.getLongitude() != null) {
            saveGpsLocation(deviceId, request.getLatitude(), request.getLongitude());
            safeZoneService.checkGeofence(
                    deviceId, request.getLatitude(), request.getLongitude()
            );
        }

        log.debug("Ping actualizado para dispositivo {}", deviceId);

        return toResponse(smartwatch);
    }

    // -------------------------------------------------------------------------
    // RF-15: Obtener estado de un smartwatch
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public SmartwatchResponse getStatus(String deviceId) {
        Smartwatch smartwatch = smartwatchRepository
                .findById(deviceId)
                .orElseThrow(() -> new IllegalArgumentException(
                    "Dispositivo no encontrado: " + deviceId
                ));

        // Actualizar connectionStatus según el último ping
        boolean isConnected = isRecentlyPinged(smartwatch.getLastPing());

        return SmartwatchResponse.builder()
                .deviceId(smartwatch.getDeviceId())
                .patientId(smartwatch.getPatient().getPatientId())
                .patientName(smartwatch.getPatient().getFullName())
                .batteryLevel(smartwatch.getBatteryLevel())
                .connectionStatus(isConnected)
                .lastPing(smartwatch.getLastPing())
                .minutesSinceLastPing(minutesSince(smartwatch.getLastPing()))
                .build();
    }

    // -------------------------------------------------------------------------
    // RF-15: Estado de todos los smartwatches de mis pacientes
    // -------------------------------------------------------------------------

        @Transactional(readOnly = true)
        public List<SmartwatchResponse> getMyDevicesStatus() {
        UUID caregiverId = getAuthenticatedCaregiverId();

        return patientRepository
                .findAllByCaregiverId(caregiverId)
                .stream()
                .flatMap(patient ->
                        // findAllByPatientPatientId devuelve List, no Optional
                        smartwatchRepository
                                .findAllByPatientPatientId(patient.getPatientId())
                                .stream()
                )
                .map(this::toResponse)
                .toList();
        }

    // -------------------------------------------------------------------------
    // RF-15: Marcar dispositivos desconectados
    // Puede llamarse desde un @Scheduled task
    // -------------------------------------------------------------------------

    @Transactional
    public void markDisconnectedDevices() {
        LocalDateTime threshold = LocalDateTime.now()
                .minusMinutes(DISCONNECT_THRESHOLD_MINUTES);

        List<Smartwatch> disconnected =
                smartwatchRepository.findDisconnected(threshold);

        disconnected.forEach(sw -> {
            if (sw.getConnectionStatus()) {
                sw.setConnectionStatus(false);
                smartwatchRepository.save(sw);
                log.warn("Dispositivo marcado como desconectado: {}",
                        sw.getDeviceId());
                sw.setConnectionStatus(false);
                smartwatchRepository.save(sw);
                webSocketNotificationService.notifyConnectionStatus(
                        sw.getDeviceId(),
                        sw.getPatient().getPatientId(),
                        false
                );
            }
        });
    }

    // -------------------------------------------------------------------------
    // GPS
    // -------------------------------------------------------------------------

    private void saveGpsLocation(
            String deviceId,
            double latitude,
            double longitude
    ) {
        GpsLocation location = GpsLocation.builder()
                .deviceId(deviceId)
                .coordinates(geometryFactory.createPoint(
                        new Coordinate(longitude, latitude)
                ))
                .build();

        gpsLocationRepository.save(location);
    }

    // -------------------------------------------------------------------------
    // Mapeo entidad → DTO
    // -------------------------------------------------------------------------

    private SmartwatchResponse toResponse(Smartwatch sw) {
        boolean isConnected = isRecentlyPinged(sw.getLastPing());

        return SmartwatchResponse.builder()
                .deviceId(sw.getDeviceId())
                .patientId(sw.getPatient().getPatientId())
                .patientName(sw.getPatient().getFullName())
                .batteryLevel(sw.getBatteryLevel())
                .connectionStatus(isConnected)
                .lastPing(sw.getLastPing())
                .minutesSinceLastPing(minutesSince(sw.getLastPing()))
                .build();
    }

    private boolean isRecentlyPinged(LocalDateTime lastPing) {
        if (lastPing == null) return false;
        return Duration.between(lastPing, LocalDateTime.now())
                .toMinutes() < DISCONNECT_THRESHOLD_MINUTES;
    }

    private Long minutesSince(LocalDateTime lastPing) {
        if (lastPing == null) return null;
        return Duration.between(lastPing, LocalDateTime.now()).toMinutes();
    }

    // -------------------------------------------------------------------------
    // Utilidades
    // -------------------------------------------------------------------------

    private UUID getAuthenticatedCaregiverId() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
        return caregiverRepository.findByEmail(email)
                .orElseThrow()
                .getCaregiverId();
    }
}