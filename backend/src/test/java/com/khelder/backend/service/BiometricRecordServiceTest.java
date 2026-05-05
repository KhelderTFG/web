package com.khelder.backend.service;

import com.khelder.backend.dto.biometric.BiometricRecordRequest;
import com.khelder.backend.dto.biometric.BiometricRecordResponse;
import com.khelder.backend.entity.BiometricRecord;
import com.khelder.backend.entity.Patient;
import com.khelder.backend.entity.Smartwatch;
import com.khelder.backend.repository.BiometricRecordRepository;
import com.khelder.backend.repository.CaregiverRepository;
import com.khelder.backend.repository.PatientRepository;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
@DisplayName("BiometricRecordService — Tests unitarios")
class BiometricRecordServiceTest {

    @Mock private BiometricRecordRepository biometricRecordRepository;
    @Mock private SmartwatchRepository      smartwatchRepository;
    @Mock private PatientRepository         patientRepository;
    @Mock private CaregiverRepository       caregiverRepository;
    @Mock private AlertService              alertService;

    @InjectMocks private BiometricRecordService biometricRecordService;

    private Smartwatch             smartwatch;
    private BiometricRecordRequest request;

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
                .build();

        request = new BiometricRecordRequest();
        request.setDeviceId("watch-maria-001");
        request.setHeartRate(75.0);
        request.setSpO2(98.0);
        request.setSteps(1200L);
        request.setTemperature(36.5);
        request.setTimestamp(System.currentTimeMillis());
    }

    // -------------------------------------------------------------------------
    // RF-03: Guardar registros biométricos
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("RF-03: Registro biométrico válido se guarda correctamente")
    void saveRecord_ValidRequest_SavesRecord() {
        BiometricRecord saved = BiometricRecord.builder()
                .recordId(UUID.randomUUID())
                .deviceId("watch-maria-001")
                .heartRate(75.0)
                .timestamp(LocalDateTime.now())
                .build();

        when(smartwatchRepository.findById("watch-maria-001"))
                .thenReturn(Optional.of(smartwatch));
        when(biometricRecordRepository.save(any())).thenReturn(saved);
        when(smartwatchRepository.updatePing(anyString(), any(), any()))
                .thenReturn(1);

        BiometricRecordResponse response =
                biometricRecordService.saveRecord(request);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("OK");
        verify(biometricRecordRepository).save(any(BiometricRecord.class));
    }

    @Test
    @DisplayName("RF-03: Registro con dispositivo inexistente lanza excepción")
    void saveRecord_DeviceNotFound_ThrowsException() {
        when(smartwatchRepository.findById(anyString()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> biometricRecordService.saveRecord(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("RF-03: FC > 140 genera alerta HEART_RATE_HIGH")
    void saveRecord_HighHeartRate_CreatesAlert() {
        request.setHeartRate(155.0);

        BiometricRecord saved = BiometricRecord.builder()
                .recordId(UUID.randomUUID())
                .deviceId("watch-maria-001")
                .heartRate(155.0)
                .timestamp(LocalDateTime.now())
                .build();

        when(smartwatchRepository.findById("watch-maria-001"))
                .thenReturn(Optional.of(smartwatch));
        when(biometricRecordRepository.save(any())).thenReturn(saved);
        when(smartwatchRepository.updatePing(anyString(), any(), any()))
                .thenReturn(1);

        biometricRecordService.saveRecord(request);

        verify(alertService).createHeartRateAlert(
                "watch-maria-001", 155.0, "HEART_RATE_HIGH",
                request.getTimestamp()
        );
    }

    @Test
    @DisplayName("RF-03: FC < 45 genera alerta HEART_RATE_LOW")
    void saveRecord_LowHeartRate_CreatesAlert() {
        request.setHeartRate(40.0);

        BiometricRecord saved = BiometricRecord.builder()
                .recordId(UUID.randomUUID())
                .deviceId("watch-maria-001")
                .heartRate(40.0)
                .timestamp(LocalDateTime.now())
                .build();

        when(smartwatchRepository.findById("watch-maria-001"))
                .thenReturn(Optional.of(smartwatch));
        when(biometricRecordRepository.save(any())).thenReturn(saved);
        when(smartwatchRepository.updatePing(anyString(), any(), any()))
                .thenReturn(1);

        biometricRecordService.saveRecord(request);

        verify(alertService).createHeartRateAlert(
                "watch-maria-001", 40.0, "HEART_RATE_LOW",
                request.getTimestamp()
        );
    }

    @Test
    @DisplayName("RF-03: FC entre 45 y 140 no genera alerta")
    void saveRecord_NormalHeartRate_NoAlert() {
        request.setHeartRate(75.0);

        BiometricRecord saved = BiometricRecord.builder()
                .recordId(UUID.randomUUID())
                .deviceId("watch-maria-001")
                .heartRate(75.0)
                .timestamp(LocalDateTime.now())
                .build();

        when(smartwatchRepository.findById("watch-maria-001"))
                .thenReturn(Optional.of(smartwatch));
        when(biometricRecordRepository.save(any())).thenReturn(saved);
        when(smartwatchRepository.updatePing(anyString(), any(), any()))
                .thenReturn(1);

        biometricRecordService.saveRecord(request);

        verify(alertService, org.mockito.Mockito.never())
                .createHeartRateAlert(
                        anyString(),
                        anyDouble(),
                        anyString(),
                        anyLong()
                );
    }
}