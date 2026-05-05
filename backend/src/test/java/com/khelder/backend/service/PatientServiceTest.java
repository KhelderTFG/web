package com.khelder.backend.service;

import com.khelder.backend.dto.patient.PatientRequest;
import com.khelder.backend.dto.patient.PatientResponse;
import com.khelder.backend.entity.Caregiver;
import com.khelder.backend.entity.CaregiverPatient;
import com.khelder.backend.entity.Patient;
import com.khelder.backend.entity.Smartwatch;
import com.khelder.backend.repository.AlertRepository;
import com.khelder.backend.repository.CaregiverPatientRepository;
import com.khelder.backend.repository.CaregiverRepository;
import com.khelder.backend.repository.MedicalHistoryRepository;
import com.khelder.backend.repository.PatientRepository;
import com.khelder.backend.repository.SmartwatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@DisplayName("PatientService — Tests unitarios")
class PatientServiceTest {

    @Mock private PatientRepository          patientRepository;
    @Mock private CaregiverRepository        caregiverRepository;
    @Mock private CaregiverPatientRepository caregiverPatientRepository;
    @Mock private MedicalHistoryRepository   medicalHistoryRepository;
    @Mock private SmartwatchRepository       smartwatchRepository;
    @Mock private AlertRepository            alertRepository;

    @InjectMocks private PatientService patientService;

    private Caregiver caregiver;
    private Patient   patient;
    private UUID      caregiverId;
    private UUID      patientId;

    @BeforeEach
    void setUp() {
        caregiverId = UUID.randomUUID();
        patientId   = UUID.randomUUID();

        caregiver = Caregiver.builder()
                .caregiverId(caregiverId)
                .email("carlos@khelder.com")
                .name("Carlos Martínez")
                .build();

        patient = Patient.builder()
                .patientId(patientId)
                .fullName("María Antonia García")
                .dateOfBirth(LocalDate.of(1945, 3, 15))
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
    // RF-09: Listar pacientes
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("RF-09: Listar pacientes del cuidador autenticado")
    void getMyPatients_ReturnsPatientsForCaregiver() {
        Smartwatch sw = Smartwatch.builder()
                .deviceId("watch-001")
                .patient(patient)
                .connectionStatus(true)
                .batteryLevel(78)
                .build();

        when(patientRepository.findAllByCaregiverId(caregiverId))
                .thenReturn(List.of(patient));
        when(smartwatchRepository.findByPatientPatientId(patientId))
                .thenReturn(Optional.of(sw));
        when(alertRepository.countActiveByCaregiverId(caregiverId))
                .thenReturn(2L);

        List<PatientResponse> result = patientService.getMyPatients();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFullName()).isEqualTo("María Antonia García");
        assertThat(result.get(0).getDeviceConnected()).isTrue();
        assertThat(result.get(0).getBatteryLevel()).isEqualTo(78);
    }

    @Test
    @DisplayName("RF-09: Cuidador sin pacientes devuelve lista vacía")
    void getMyPatients_NoPatientsAssigned_ReturnsEmptyList() {
        when(patientRepository.findAllByCaregiverId(caregiverId))
                .thenReturn(List.of());

        List<PatientResponse> result = patientService.getMyPatients();

        assertThat(result).isEmpty();
    }

    // -------------------------------------------------------------------------
    // RF-09: Crear paciente
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("RF-09: Crear paciente lo asigna al cuidador autenticado")
    void createPatient_AssignsToCaregiverAndSaves() {
        PatientRequest request = new PatientRequest();
        request.setFullName("Antonio Ruiz");
        request.setDateOfBirth(LocalDate.of(1940, 5, 20));

        when(caregiverRepository.findById(caregiverId))
                .thenReturn(Optional.of(caregiver));
        when(patientRepository.save(any())).thenReturn(patient);
        when(caregiverPatientRepository.save(any()))
                .thenReturn(mock(CaregiverPatient.class));
        when(medicalHistoryRepository.findByPatientPatientId(any()))
                .thenReturn(Optional.empty());
        when(smartwatchRepository.findByPatientPatientId(any()))
                .thenReturn(Optional.empty());

        var result = patientService.createPatient(request);

        assertThat(result).isNotNull();
        assertThat(result.getFullName()).isEqualTo("María Antonia García");
    }

    // -------------------------------------------------------------------------
    // RF-09: Control de acceso
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("RF-09: Acceso a paciente sin permiso lanza SecurityException")
    void getPatientDetail_NoPermission_ThrowsSecurityException() {
        when(patientRepository.existsAssignment(caregiverId, patientId))
                .thenReturn(false);

        assertThatThrownBy(() -> patientService.getPatientDetail(patientId))
                .isInstanceOf(SecurityException.class);
    }
}