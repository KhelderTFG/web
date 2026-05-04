package com.khelder.backend.dto.patient;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Respuesta resumida — usada en listados del dashboard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientResponse {
    private UUID      patientId;
    private String    fullName;
    private LocalDate dateOfBirth;
    private Integer   age;
    private String    activeDeviceId;
    private Boolean   deviceConnected;
    private Integer   batteryLevel;
    private Long      activeAlertsCount;
}