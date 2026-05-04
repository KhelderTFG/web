package com.khelder.backend.dto.smartwatch;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class SmartwatchRequest {

    @NotBlank(message = "El device_id es obligatorio")
    private String deviceId;

    @NotNull(message = "El patient_id es obligatorio")
    private UUID patientId;
}