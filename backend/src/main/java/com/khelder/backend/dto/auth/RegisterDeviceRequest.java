package com.khelder.backend.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterDeviceRequest {
    @NotBlank
    @Size(min = 6, max = 6)
    @JsonProperty("pairingCode")
    private String pairingCode;
}