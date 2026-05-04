package com.khelder.backend.controller;

import com.khelder.backend.dto.medicalhistory.MedicalHistoryRequest;
import com.khelder.backend.dto.medicalhistory.MedicalHistoryResponse;
import com.khelder.backend.dto.patient.PatientDetailResponse;
import com.khelder.backend.dto.patient.PatientRequest;
import com.khelder.backend.dto.patient.PatientResponse;
import com.khelder.backend.service.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;

    /**
     * RF-09: Listar pacientes del cuidador autenticado
     * GET /api/v1/patients
     */
    @GetMapping
    public ResponseEntity<List<PatientResponse>> getMyPatients() {
        return ResponseEntity.ok(patientService.getMyPatients());
    }

    /**
     * RF-09: Detalle de un paciente
     * GET /api/v1/patients/{patientId}
     */
    @GetMapping("/{patientId}")
    public ResponseEntity<PatientDetailResponse> getPatientDetail(
            @PathVariable UUID patientId
    ) {
        return ResponseEntity.ok(patientService.getPatientDetail(patientId));
    }

    /**
     * RF-09: Crear nuevo paciente
     * POST /api/v1/patients
     */
    @PostMapping
    public ResponseEntity<PatientDetailResponse> createPatient(
            @Valid @RequestBody PatientRequest request
    ) {
        PatientDetailResponse response = patientService.createPatient(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * RF-09: Actualizar datos de un paciente
     * PUT /api/v1/patients/{patientId}
     */
    @PutMapping("/{patientId}")
    public ResponseEntity<PatientDetailResponse> updatePatient(
            @PathVariable UUID patientId,
            @Valid @RequestBody PatientRequest request
    ) {
        return ResponseEntity.ok(patientService.updatePatient(patientId, request));
    }

    /**
     * RF-09: Actualizar historial médico
     * PUT /api/v1/patients/{patientId}/medical-history
     */
    @PutMapping("/{patientId}/medical-history")
    public ResponseEntity<MedicalHistoryResponse> updateMedicalHistory(
            @PathVariable UUID patientId,
            @RequestBody MedicalHistoryRequest request
    ) {
        return ResponseEntity.ok(
                patientService.updateMedicalHistory(patientId, request)
        );
    }

    /**
     * RF-09: Asignar cuidador adicional
     * POST /api/v1/patients/{patientId}/caregivers/{caregiverId}
     */
    @PostMapping("/{patientId}/caregivers/{caregiverId}")
    public ResponseEntity<Void> assignCaregiver(
            @PathVariable UUID patientId,
            @PathVariable UUID caregiverId
    ) {
        patientService.assignCaregiver(patientId, caregiverId);
        return ResponseEntity.ok().build();
    }

    /**
     * RF-09: Eliminar paciente
     * DELETE /api/v1/patients/{patientId}
     */
    @DeleteMapping("/{patientId}")
    public ResponseEntity<Void> deletePatient(
            @PathVariable UUID patientId
    ) {
        patientService.deletePatient(patientId);
        return ResponseEntity.noContent().build();
    }
}