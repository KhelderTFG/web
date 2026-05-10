package com.khelder.backend.service;

import com.khelder.backend.dto.safezone.SafeZoneRequest;
import com.khelder.backend.dto.safezone.SafeZoneResponse;
import com.khelder.backend.entity.Caregiver;
import com.khelder.backend.entity.GpsLocation;
import com.khelder.backend.entity.Patient;
import com.khelder.backend.entity.SafeZone;
import com.khelder.backend.entity.Smartwatch;
import com.khelder.backend.repository.CaregiverRepository;
import com.khelder.backend.repository.GpsLocationRepository;
import com.khelder.backend.repository.PatientRepository;
import com.khelder.backend.repository.SafeZoneRepository;
import com.khelder.backend.repository.SmartwatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SafeZoneService {

    private final SafeZoneRepository  safeZoneRepository;
    private final PatientRepository   patientRepository;
    private final CaregiverRepository caregiverRepository;
    private final SmartwatchRepository smartwatchRepository;
    private final GpsLocationRepository gpsLocationRepository;
    private final AlertService        alertService;

    // SRID 4326 = WGS84 (sistema de coordenadas GPS estándar)
    private static final int SRID = 4326;
    private final GeometryFactory geometryFactory =
            new GeometryFactory(new PrecisionModel(), SRID);

    // -------------------------------------------------------------------------
    // RF-14: Crear zona segura
    // -------------------------------------------------------------------------

    @Transactional
    public SafeZoneResponse createSafeZone(SafeZoneRequest request) {
        UUID caregiverId = getAuthenticatedCaregiverId();

        if (!patientRepository.existsAssignment(caregiverId, request.getPatientId())) {
            throw new SecurityException(
                "No tienes permisos para configurar zonas para este paciente"
            );
        }

        Patient patient = patientRepository
                .findById(request.getPatientId())
                .orElseThrow(() -> new IllegalArgumentException(
                    "Paciente no encontrado: " + request.getPatientId()
                ));

        Caregiver caregiver = caregiverRepository
                .findById(caregiverId)
                .orElseThrow();

        // Crear el punto espacial con JTS — PostGIS lo persiste como geometry
        // MakePoint(longitud, latitud) — el orden es lon, lat en PostGIS
        Point centroid = geometryFactory.createPoint(
                new Coordinate(request.getLongitude(), request.getLatitude())
        );

        SafeZone safeZone = SafeZone.builder()
                .patient(patient)
                .caregiver(caregiver)
                .centroid(centroid)
                .radiusMeters(request.getRadiusMeters())
                .build();

        SafeZone saved = safeZoneRepository.save(safeZone);

        log.info("Zona segura creada: {} para paciente {}",
                saved.getZoneId(), request.getPatientId());

        return toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // RF-14: Listar zonas seguras de un paciente
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<SafeZoneResponse> getSafeZonesByPatient(UUID patientId) {
        UUID caregiverId = getAuthenticatedCaregiverId();

        if (!patientRepository.existsAssignment(caregiverId, patientId)) {
            throw new SecurityException(
                "No tienes permisos para ver las zonas de este paciente"
            );
        }

        return safeZoneRepository
                .findByPatientPatientId(patientId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // -------------------------------------------------------------------------
    // RF-14: Eliminar zona segura
    // -------------------------------------------------------------------------

    @Transactional
    public void deleteSafeZone(UUID zoneId) {
        UUID caregiverId = getAuthenticatedCaregiverId();

        SafeZone zone = safeZoneRepository.findById(zoneId)
                .orElseThrow(() -> new IllegalArgumentException(
                    "Zona segura no encontrada: " + zoneId
                ));

        if (!zone.getCaregiver().getCaregiverId().equals(caregiverId)) {
            throw new SecurityException(
                "No tienes permisos para eliminar esta zona"
            );
        }

        safeZoneRepository.deleteById(zoneId);
        log.info("Zona segura eliminada: {}", zoneId);
    }

    // -------------------------------------------------------------------------
    // RF-14: Verificar si un dispositivo está dentro de sus zonas seguras
    // Llamado cada vez que llega una nueva ubicación GPS del reloj
    // -------------------------------------------------------------------------

    @Transactional
    public void checkGeofence(String deviceId, double latitude, double longitude) {

        // Obtener el paciente del dispositivo
        Smartwatch smartwatch = smartwatchRepository.findById(deviceId)
        .orElseGet(() -> smartwatchRepository.findByNodeId(deviceId)
                .orElse(null));

        if (smartwatch == null) return;

        UUID patientId = smartwatch.getPatient().getPatientId();

        // Verificar si hay zonas seguras definidas
        List<SafeZone> zones = safeZoneRepository
                .findByPatientPatientId(patientId);

        if (zones.isEmpty()) return;

        // Comprobar si está fuera de todas las zonas usando PostGIS
        boolean outsideAll = safeZoneRepository
                .isOutsideAllSafeZones(patientId, latitude, longitude);

        if (outsideAll) {
            log.warn("Dispositivo {} fuera de zona segura. Lat: {}, Lon: {}",
                    deviceId, latitude, longitude);

            alertService.createGeofenceAlert(deviceId, latitude, longitude);
        }
    }

    // -------------------------------------------------------------------------
    // Mapeo entidad → DTO
    // -------------------------------------------------------------------------

    private SafeZoneResponse toResponse(SafeZone zone) {
        Double lat = null;
        Double lon = null;

        if (zone.getCentroid() != null) {
            // JTS Point: X = longitud, Y = latitud
            lon = zone.getCentroid().getX();
            lat = zone.getCentroid().getY();
        }

        return SafeZoneResponse.builder()
                .zoneId(zone.getZoneId())
                .patientId(zone.getPatient().getPatientId())
                .patientName(zone.getPatient().getFullName())
                .caregiverId(zone.getCaregiver().getCaregiverId())
                .latitude(lat)
                .longitude(lon)
                .radiusMeters(zone.getRadiusMeters())
                .build();
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