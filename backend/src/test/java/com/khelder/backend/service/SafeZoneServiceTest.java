package com.khelder.backend.service;

import com.khelder.backend.dto.safezone.SafeZoneRequest;
import com.khelder.backend.dto.safezone.SafeZoneResponse;
import com.khelder.backend.entity.Caregiver;
import com.khelder.backend.entity.Patient;
import com.khelder.backend.entity.SafeZone;
import com.khelder.backend.entity.Smartwatch;
import com.khelder.backend.repository.CaregiverRepository;
import com.khelder.backend.repository.GpsLocationRepository;
import com.khelder.backend.repository.PatientRepository;
import com.khelder.backend.repository.SafeZoneRepository;
import com.khelder.backend.repository.SmartwatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@DisplayName("SafeZoneService — Tests unitarios")
class SafeZoneServiceTest {

    @Mock private SafeZoneRepository   safeZoneRepository;
    @Mock private PatientRepository    patientRepository;
    @Mock private CaregiverRepository  caregiverRepository;
    @Mock private SmartwatchRepository smartwatchRepository;
    @Mock private GpsLocationRepository gpsLocationRepository;
    @Mock private AlertService         alertService;

    @InjectMocks private SafeZoneService safeZoneService;

    private Caregiver  caregiver;
    private Patient    patient;
    private SafeZone   safeZone;
    private Smartwatch smartwatch;
    private UUID       caregiverId;
    private UUID       patientId;
    private UUID       zoneId;

    private final GeometryFactory geometryFactory =
            new GeometryFactory(new PrecisionModel(), 4326);

    @BeforeEach
    void setUp() {
        caregiverId = UUID.randomUUID();
        patientId   = UUID.randomUUID();
        zoneId      = UUID.randomUUID();

        caregiver = Caregiver.builder()
                .caregiverId(caregiverId)
                .email("carlos@khelder.com")
                .name("Carlos Martínez")
                .build();

        patient = Patient.builder()
                .patientId(patientId)
                .fullName("María Antonia García")
                .build();

        smartwatch = Smartwatch.builder()
                .deviceId("watch-maria-001")
                .patient(patient)
                .connectionStatus(true)
                .build();

        safeZone = SafeZone.builder()
                .zoneId(zoneId)
                .patient(patient)
                .caregiver(caregiver)
                .centroid(geometryFactory.createPoint(
                        new Coordinate(-5.9845, 37.3891)
                ))
                .radiusMeters(200)
                .build();

        // Simular cuidador autenticado
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("carlos@khelder.com");
        SecurityContext secCtx = mock(SecurityContext.class);
        when(secCtx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(secCtx);

        when(caregiverRepository.findByEmail("carlos@khelder.com"))
                .thenReturn(Optional.of(caregiver));
    }

    // -------------------------------------------------------------------------
    // RF-14: Crear zona segura
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("RF-14: Crear zona segura válida la persiste correctamente")
    void createSafeZone_Valid_SavesZone() {
        SafeZoneRequest request = new SafeZoneRequest();
        request.setPatientId(patientId);
        request.setLatitude(37.3891);
        request.setLongitude(-5.9845);
        request.setRadiusMeters(200);

        when(patientRepository.existsAssignment(caregiverId, patientId))
                .thenReturn(true);
        when(patientRepository.findById(patientId))
                .thenReturn(Optional.of(patient));
        when(caregiverRepository.findById(caregiverId))
                .thenReturn(Optional.of(caregiver));
        when(safeZoneRepository.save(any())).thenReturn(safeZone);

        SafeZoneResponse response = safeZoneService.createSafeZone(request);

        assertThat(response).isNotNull();
        assertThat(response.getRadiusMeters()).isEqualTo(200);
        assertThat(response.getLatitude()).isEqualTo(37.3891);
        assertThat(response.getLongitude()).isEqualTo(-5.9845);
        verify(safeZoneRepository).save(any(SafeZone.class));
    }

    @Test
    @DisplayName("RF-14: Crear zona sin permisos lanza SecurityException")
    void createSafeZone_NoPermission_ThrowsSecurityException() {
        SafeZoneRequest request = new SafeZoneRequest();
        request.setPatientId(patientId);
        request.setLatitude(37.3891);
        request.setLongitude(-5.9845);
        request.setRadiusMeters(200);

        when(patientRepository.existsAssignment(caregiverId, patientId))
                .thenReturn(false);

        assertThatThrownBy(() -> safeZoneService.createSafeZone(request))
                .isInstanceOf(SecurityException.class);
    }

    // -------------------------------------------------------------------------
    // RF-14: Listar zonas seguras
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("RF-14: Listar zonas de un paciente devuelve lista correcta")
    void getSafeZonesByPatient_ReturnsList() {
        when(patientRepository.existsAssignment(caregiverId, patientId))
                .thenReturn(true);
        when(safeZoneRepository.findByPatientPatientId(patientId))
                .thenReturn(List.of(safeZone));

        List<SafeZoneResponse> result =
                safeZoneService.getSafeZonesByPatient(patientId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRadiusMeters()).isEqualTo(200);
    }

    // -------------------------------------------------------------------------
    // RF-14: Eliminar zona segura
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("RF-14: Eliminar zona propia funciona correctamente")
    void deleteSafeZone_OwnedZone_DeletesSuccessfully() {
        when(safeZoneRepository.findById(zoneId))
                .thenReturn(Optional.of(safeZone));

        safeZoneService.deleteSafeZone(zoneId);

        verify(safeZoneRepository).deleteById(zoneId);
    }

    @Test
    @DisplayName("RF-14: Eliminar zona ajena lanza SecurityException")
    void deleteSafeZone_NotOwned_ThrowsSecurityException() {
        Caregiver otherCaregiver = Caregiver.builder()
                .caregiverId(UUID.randomUUID())
                .build();

        SafeZone otherZone = SafeZone.builder()
                .zoneId(zoneId)
                .caregiver(otherCaregiver)
                .patient(patient)
                .radiusMeters(100)
                .build();

        when(safeZoneRepository.findById(zoneId))
                .thenReturn(Optional.of(otherZone));

        assertThatThrownBy(() -> safeZoneService.deleteSafeZone(zoneId))
                .isInstanceOf(SecurityException.class);
    }

    // -------------------------------------------------------------------------
    // RF-14: Verificación de geofence
    // -------------------------------------------------------------------------

        @Test
        @DisplayName("RF-14: Dispositivo dentro de zona no genera alerta")
        void checkGeofence_InsideZone_NoAlert() {
                when(smartwatchRepository.findById("watch-maria-001"))
                        .thenReturn(Optional.of(smartwatch));
                when(safeZoneRepository.findByPatientPatientId(patientId))
                        .thenReturn(List.of(safeZone));
                when(safeZoneRepository.isOutsideAllSafeZones(
                        eq(patientId), anyDouble(), anyDouble()
                )).thenReturn(false);

                safeZoneService.checkGeofence("watch-maria-001", 37.3891, -5.9845);

                verify(alertService, never()).createGeofenceAlert(
                        anyString(), anyDouble(), anyDouble()
                );
        }

        @Test
        @DisplayName("RF-14: Dispositivo fuera de zona genera alerta GEOFENCE_EXIT")
        void checkGeofence_OutsideZone_CreatesAlert() {
                when(smartwatchRepository.findById("watch-maria-001"))
                        .thenReturn(Optional.of(smartwatch));
                when(safeZoneRepository.findByPatientPatientId(patientId))
                        .thenReturn(List.of(safeZone));
                when(safeZoneRepository.isOutsideAllSafeZones(
                        patientId, 40.4168, -3.7038)
                ).thenReturn(true);

                safeZoneService.checkGeofence("watch-maria-001", 40.4168, -3.7038);

                verify(alertService).createGeofenceAlert(
                        "watch-maria-001", 40.4168, -3.7038
                );
    }
        @Test
        @DisplayName("RF-14: Dispositivo sin zonas definidas no genera alerta")
        void checkGeofence_NoZonesDefined_NoAlert() {
        when(smartwatchRepository.findById("watch-maria-001"))
                .thenReturn(Optional.of(smartwatch));
        when(safeZoneRepository.findByPatientPatientId(patientId))
                .thenReturn(List.of());

        safeZoneService.checkGeofence("watch-maria-001", 40.4168, -3.7038);

        verify(safeZoneRepository, never())
                .isOutsideAllSafeZones(any(UUID.class), anyDouble(), anyDouble());
        }
}