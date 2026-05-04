package com.khelder.backend.dto.alert;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertResponse {
    private UUID          alertId;
    private String        deviceId;
    private String        patientName;
    private UUID          patientId;
    private String        alertType;
    private String        status;
    private Double        latitude;
    private Double        longitude;
    private Double        heartRate;
    private Integer       batteryLevel;
    private LocalDateTime timestamp;
    private String        message;
}