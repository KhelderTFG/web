package com.khelder.backend.service;

import com.khelder.backend.dto.alert.AlertRequest;
import com.khelder.backend.dto.alert.AlertResponse;
import com.khelder.backend.entity.Alert;
import com.khelder.backend.entity.Patient;
import com.khelder.backend.entity.Smartwatch;
import com.khelder.backend.repository.AlertRepository;
import com.khelder.backend.repository.CaregiverRepository;
import com.khelder.backend.repository.SmartwatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AlertService — Tests unitarios")
class AlertServiceTest {

    @Mock private AlertRepository                alertRepository;
    @Mock private SmartwatchRepository           smartwatchRepository;
    @Mock private CaregiverRepository            caregiverRepository;
    @Mock private WebSocketNotificationService   webSocketNotificationService;

    @InjectMocks private AlertService alertService;

    private Smartwatch  smartwatch;
    private AlertRequest alertRequest;

    @BeforeEach
    void setUp() {
        Patient patient = Patient.builder()
                .patientId(UUID.randomUUID())
                .fullName("María Antonia García")
                .build();

        smartwatch = Smartwatch.builder()
                .deviceId("watch-maria-001")
                .patient(patient)
                .connectionStatus(true)
                .batteryLevel(78)
                .build();

        alertRequest = new AlertRequest();
        alertRequest.setDeviceId("watch-maria-001");
        alertRequest.setAlertType("SOS_MANUAL");
        alertRequest.setLatitude(37.3891);
        alertRequest.setLongitude(-5.9845);
        alertRequest.setTimestamp(System.currentTimeMillis());
    }

    // -------------------------------------------------------------------------
    // RF-01: SOS Manual
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("RF-01: Alerta SOS se guarda con estado ACTIVE")
    void saveAlert_SOS_SavedAsActive() {
        Alert savedAlert = Alert.builder()
                .alertId(UUID.randomUUID())
                .deviceId("watch-maria-001")
                .alertType("SOS_MANUAL")
                .status("ACTIVE")
                .timestamp(LocalDateTime.now())
                .build();

        when(smartwatchRepository.findById("watch-maria-001"))
                .thenReturn(Optional.of(smartwatch));
        when(alertRepository.save(any(Alert.class))).thenReturn(savedAlert);

        AlertResponse response = alertService.saveAlert(alertRequest);

        assertThat(response).isNotNull();
        assertThat(response.getAlertType()).isEqualTo("SOS_MANUAL");
        assertThat(response.getStatus()).isEqualTo("ACTIVE");  // estado de la alerta
        verify(alertRepository).save(any(Alert.class));
        verify(webSocketNotificationService).notifyAlert(any(Alert.class));
        }

    @Test
    @DisplayName("RF-01: Alerta con dispositivo inexistente lanza excepción")
    void saveAlert_DeviceNotFound_ThrowsException() {
        when(smartwatchRepository.findById("watch-inexistente"))
                .thenReturn(Optional.empty());

        alertRequest.setDeviceId("watch-inexistente");

        assertThatThrownBy(() -> alertService.saveAlert(alertRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("watch-inexistente");
    }

    // -------------------------------------------------------------------------
    // RF-12: Resolución de alertas
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("RF-12: Resolver alerta ACTIVE cambia estado a RESOLVED")
    void resolveAlert_ActiveAlert_ChangesStatusToResolved() {
        UUID alertId = UUID.randomUUID();
        Alert activeAlert = Alert.builder()
                .alertId(alertId)
                .deviceId("watch-maria-001")
                .alertType("SOS_MANUAL")
                .status("ACTIVE")
                .timestamp(LocalDateTime.now())
                .build();

        when(alertRepository.findById(alertId))
                .thenReturn(Optional.of(activeAlert));
        when(alertRepository.save(any())).thenReturn(activeAlert);
        when(smartwatchRepository.findById(any())).thenReturn(Optional.of(smartwatch));

        alertService.resolveAlert(alertId, new com.khelder.backend.dto.alert.AlertResolveRequest());

        assertThat(activeAlert.getStatus()).isEqualTo("RESOLVED");
        verify(alertRepository).save(activeAlert);
    }

    @Test
    @DisplayName("RF-12: Resolver alerta ya RESOLVED lanza excepción")
    void resolveAlert_AlreadyResolved_ThrowsException() {
        UUID alertId = UUID.randomUUID();
        Alert resolvedAlert = Alert.builder()
                .alertId(alertId)
                .status("RESOLVED")
                .build();

        when(alertRepository.findById(alertId))
                .thenReturn(Optional.of(resolvedAlert));

        assertThatThrownBy(() -> alertService.resolveAlert(
                alertId,
                new com.khelder.backend.dto.alert.AlertResolveRequest()
        ))
                .isInstanceOf(IllegalStateException.class);
    }
}