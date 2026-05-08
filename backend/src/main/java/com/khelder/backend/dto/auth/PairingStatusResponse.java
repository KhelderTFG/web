package com.khelder.backend.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PairingStatusResponse {
    private String  status;   // "PENDING" | "APPROVED"
    private String  token;    // solo si APPROVED
    private String  deviceId; // solo si APPROVED
}