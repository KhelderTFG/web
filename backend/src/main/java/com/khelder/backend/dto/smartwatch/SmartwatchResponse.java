package com.khelder.backend.dto.smartwatch;

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
public class SmartwatchResponse {
    private String        deviceId;
    private UUID          patientId;
    private String        patientName;
    private Integer       batteryLevel;
    private Boolean       connectionStatus;
    private LocalDateTime lastPing;
    private Long          minutesSinceLastPing;
}