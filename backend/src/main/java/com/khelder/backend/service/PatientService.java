package com.khelder.backend.service;

import com.khelder.backend.dto.medicalhistory.MedicalHistoryRequest;
import com.khelder.backend.dto.medicalhistory.MedicalHistoryResponse;
import com.khelder.backend.dto.patient.PatientDetailResponse;
import com.khelder.backend.dto.patient.PatientRequest;
import com.khelder.backend.dto.patient.PatientResponse;
import com.khelder.backend.entity.Caregiver;
import com.khelder.backend.entity.CaregiverPatient;
import com.khelder.backend.entity.CaregiverPatientId;
import com.khelder.backend.entity.MedicalHistory;
import com.khelder.backend.entity.Patient;
import com.khelder.backend.entity.Smartwatch;
import com.khelder.backend.repository.AlertRepository;
import com.khelder.backend.repository.CaregiverPatientRepository;
import com.khelder.backend.repository.CaregiverRepository;
import com.khelder.backend.repository.MedicalHistoryRepository;
import com.khelder.backend.repository.PatientRepository;
import com.khelder.backend.repository.SmartwatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository          patientRepository;
    private final CaregiverRepository        caregiverRepository;
    private final CaregiverPatientRepository caregiverPatientRepository;
    private final MedicalHistoryRepository   medicalHistoryRepository;
    private final SmartwatchRepository       smartwatchRepository;
    private final AlertRepository            alertRepository;

    // -------------------------------------------------------------------------
    // RF-09: Listar pacientes del cuidador autenticado
    // -------------------------------------------------------------------------

    public List<PatientResponse> getMyPatients() {
        UUID caregiverId = getAuthenticatedCaregiverId();

        return patientRepository
                .findAllByCaregiverId(caregiverId)
                .stream()
                .map(patient -> toPatientResponse(patient, caregiverId))
                .toList();
    }

    // -------------------------------------------------------------------------
    // RF-09: Obtener detalle de un paciente
    // -------------------------------------------------------------------------

    public PatientDetailResponse getPatientDetail(UUID patientId) {
        UUID caregiverId = getAuthenticatedCaregiverId();

        // Verificar que el cuidador tiene acceso a este paciente
        if (!patientRepository.existsAssignment(caregiverId, patientId)) {
            throw new SecurityException(
                "No tienes permisos para ver este paciente"
            );
        }

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException(
                    "Paciente no encontrado: " + patientId
                ));

        MedicalHistory history = medicalHistoryRepository
                .findByPatientPatientId(patientId)
                .orElse(null);

        Smartwatch smartwatch = smartwatchRepository
                .findByPatientPatientId(patientId)
                .orElse(null);

        return PatientDetailResponse.builder()
                .patientId(patient.getPatientId())
                .fullName(patient.getFullName())
                .dateOfBirth(patient.getDateOfBirth())
                .age(calculateAge(patient.getDateOfBirth()))
                .activeDeviceId(smartwatch != null ? smartwatch.getDeviceId() : null)
                .deviceConnected(smartwatch != null && smartwatch.getConnectionStatus())
                .batteryLevel(smartwatch != null ? smartwatch.getBatteryLevel() : null)
                .medicalHistory(history != null ? toMedicalHistoryResponse(history) : null)
                .build();
    }

    // -------------------------------------------------------------------------
    // RF-09: Crear nuevo paciente
    // -------------------------------------------------------------------------

    @Transactional
    public PatientDetailResponse createPatient(PatientRequest request) {
        UUID caregiverId = getAuthenticatedCaregiverId();

        Caregiver caregiver = caregiverRepository.findById(caregiverId)
                .orElseThrow();

        // Crear paciente
        Patient patient = Patient.builder()
                .fullName(request.getFullName())
                .dateOfBirth(request.getDateOfBirth())
                .build();

        Patient saved = patientRepository.save(patient);

        // Asignar automáticamente al cuidador que lo crea
        CaregiverPatient assignment = CaregiverPatient.builder()
                .id(new CaregiverPatientId(caregiverId, saved.getPatientId()))
                .caregiver(caregiver)
                .patient(saved)
                .build();

        caregiverPatientRepository.save(assignment);

        log.info("Paciente creado: {} por cuidador {}",
                saved.getPatientId(), caregiverId);

        return PatientDetailResponse.builder()
                .patientId(saved.getPatientId())
                .fullName(saved.getFullName())
                .dateOfBirth(saved.getDateOfBirth())
                .age(calculateAge(saved.getDateOfBirth()))
                .deviceConnected(false)
                .build();
    }

    // -------------------------------------------------------------------------
    // RF-09: Actualizar datos de un paciente
    // -------------------------------------------------------------------------

    @Transactional
    public PatientDetailResponse updatePatient(
            UUID patientId,
            PatientRequest request
    ) {
        UUID caregiverId = getAuthenticatedCaregiverId();

        if (!patientRepository.existsAssignment(caregiverId, patientId)) {
            throw new SecurityException(
                "No tienes permisos para modificar este paciente"
            );
        }

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException(
                    "Paciente no encontrado: " + patientId
                ));

        patient.setFullName(request.getFullName());
        patient.setDateOfBirth(request.getDateOfBirth());
        patientRepository.save(patient);

        return getPatientDetail(patientId);
    }

    // -------------------------------------------------------------------------
    // RF-09: Actualizar historial médico
    // -------------------------------------------------------------------------

    @Transactional
    public MedicalHistoryResponse updateMedicalHistory(
            UUID patientId,
            MedicalHistoryRequest request
    ) {
        UUID caregiverId = getAuthenticatedCaregiverId();

        if (!patientRepository.existsAssignment(caregiverId, patientId)) {
            throw new SecurityException(
                "No tienes permisos para modificar este paciente"
            );
        }

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException(
                    "Paciente no encontrado: " + patientId
                ));

        // Actualizar o crear historial médico
        MedicalHistory history = medicalHistoryRepository
                .findByPatientPatientId(patientId)
                .orElse(MedicalHistory.builder().patient(patient).build());

        history.setBloodType(request.getBloodType());
        history.setAllergies(request.getAllergies());
        history.setChronicConditions(request.getChronicConditions());
        history.setEmergencyInstructions(request.getEmergencyInstructions());

        MedicalHistory saved = medicalHistoryRepository.save(history);

        return toMedicalHistoryResponse(saved);
    }

    // -------------------------------------------------------------------------
    // RF-09: Asignar cuidador adicional a un paciente
    // -------------------------------------------------------------------------

    @Transactional
    public void assignCaregiver(UUID patientId, UUID newCaregiverId) {
        UUID caregiverId = getAuthenticatedCaregiverId();

        if (!patientRepository.existsAssignment(caregiverId, patientId)) {
            throw new SecurityException(
                "No tienes permisos para gestionar este paciente"
            );
        }

        if (caregiverPatientRepository
                .existsByIdCaregiverIdAndIdPatientId(newCaregiverId, patientId)) {
            throw new IllegalArgumentException(
                "El cuidador ya está asignado a este paciente"
            );
        }

        Caregiver newCaregiver = caregiverRepository
                .findById(newCaregiverId)
                .orElseThrow(() -> new IllegalArgumentException(
                    "Cuidador no encontrado: " + newCaregiverId
                ));

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow();

        CaregiverPatient assignment = CaregiverPatient.builder()
                .id(new CaregiverPatientId(newCaregiverId, patientId))
                .caregiver(newCaregiver)
                .patient(patient)
                .build();

        caregiverPatientRepository.save(assignment);
    }

    // -------------------------------------------------------------------------
    // RF-09: Eliminar paciente
    // -------------------------------------------------------------------------

    @Transactional
    public void deletePatient(UUID patientId) {
        UUID caregiverId = getAuthenticatedCaregiverId();

        if (!patientRepository.existsAssignment(caregiverId, patientId)) {
            throw new SecurityException(
                "No tienes permisos para eliminar este paciente"
            );
        }

        patientRepository.deleteById(patientId);
        log.info("Paciente eliminado: {}", patientId);
    }

    // -------------------------------------------------------------------------
    // Utilidades privadas
    // -------------------------------------------------------------------------

    private UUID getAuthenticatedCaregiverId() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        return caregiverRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException(
                    "Cuidador autenticado no encontrado"
                ))
                .getCaregiverId();
    }

    private PatientResponse toPatientResponse(Patient patient, UUID caregiverId) {
        Smartwatch smartwatch = smartwatchRepository
                .findByPatientPatientId(patient.getPatientId())
                .orElse(null);

        long activeAlerts = alertRepository
                .countActiveByCaregiverId(caregiverId);

        return PatientResponse.builder()
                .patientId(patient.getPatientId())
                .fullName(patient.getFullName())
                .dateOfBirth(patient.getDateOfBirth())
                .age(calculateAge(patient.getDateOfBirth()))
                .activeDeviceId(smartwatch != null ? smartwatch.getDeviceId() : null)
                .deviceConnected(smartwatch != null && smartwatch.getConnectionStatus())
                .batteryLevel(smartwatch != null ? smartwatch.getBatteryLevel() : null)
                .activeAlertsCount(activeAlerts)
                .build();
    }

    private MedicalHistoryResponse toMedicalHistoryResponse(MedicalHistory h) {
        return MedicalHistoryResponse.builder()
                .historyId(h.getHistoryId())
                .bloodType(h.getBloodType())
                .allergies(h.getAllergies())
                .chronicConditions(h.getChronicConditions())
                .emergencyInstructions(h.getEmergencyInstructions())
                .build();
    }

    private Integer calculateAge(LocalDate dateOfBirth) {
        if (dateOfBirth == null) return null;
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }
}