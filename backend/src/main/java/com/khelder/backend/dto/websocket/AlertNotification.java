package com.khelder.backend.dto.websocket;

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
public class AlertNotification {
    private UUID          alertId;
    private String        deviceId;
    private UUID          patientId;
    private String        patientName;
    private String        alertType;
    private String        status;
    private Double        latitude;
    private Double        longitude;
    private Double        heartRate;
    private Integer       batteryLevel;
    private LocalDateTime timestamp;
}