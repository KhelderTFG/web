package com.khelder.backend.dto.biometric;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BiometricRecordResponse {
    private UUID   recordId;
    private String status;
    private String message;
}