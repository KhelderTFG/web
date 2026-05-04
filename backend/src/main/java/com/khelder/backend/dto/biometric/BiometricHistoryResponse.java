package com.khelder.backend.dto.biometric;

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
public class BiometricHistoryResponse {
    private UUID          recordId;
    private String        deviceId;
    private Double        heartRate;
    private Double        spO2;
    private Long          steps;
    private Double        temperature;
    private LocalDateTime timestamp;
}