package com.khelder.backend.dto.biometric;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BiometricRecordRequest {

    @NotBlank(message = "El device_id es obligatorio")
    private String deviceId;

    private Double heartRate;
    private Double spO2;
    private Long   steps;
    private Double temperature;
    private Integer batteryLevel;
    private Double latitude;
    private Double longitude;

    @NotNull(message = "El timestamp es obligatorio")
    private Long timestamp;
}