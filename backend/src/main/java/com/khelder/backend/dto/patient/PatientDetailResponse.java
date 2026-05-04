package com.khelder.backend.dto.patient;

import com.khelder.backend.dto.medicalhistory.MedicalHistoryResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Respuesta detallada — usada en la vista individual del paciente.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientDetailResponse {
    private UUID                  patientId;
    private String                fullName;
    private LocalDate             dateOfBirth;
    private Integer               age;
    private String                activeDeviceId;
    private Boolean               deviceConnected;
    private Integer               batteryLevel;
    private MedicalHistoryResponse medicalHistory;
}