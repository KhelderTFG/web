package com.khelder.backend.dto.medicalhistory;

import lombok.Data;

@Data
public class MedicalHistoryRequest {
    private String bloodType;
    private String allergies;
    private String chronicConditions;
    private String emergencyInstructions;
}