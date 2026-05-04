package com.khelder.backend.dto.alert;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AlertRequest {

    @NotBlank(message = "El device_id es obligatorio")
    private String deviceId;

    @NotBlank(message = "El alert_type es obligatorio")
    private String alertType;

    private Double  latitude;
    private Double  longitude;
    private Double  heartRate;
    private Integer batteryLevel;

    @NotNull(message = "El timestamp es obligatorio")
    private Long timestamp;
}