package com.khelder.backend.dto.auth;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ApprovePairingRequest {
    @NotBlank
    @Size(min = 6, max = 6)
    @JsonProperty("pairing_code")
    private String pairingCode;

    @NotNull
    @JsonProperty("patient_id")
    private UUID patientId;
}