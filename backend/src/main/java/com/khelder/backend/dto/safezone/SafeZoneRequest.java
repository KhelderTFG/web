package com.khelder.backend.dto.safezone;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class SafeZoneRequest {

    @NotNull(message = "El patient_id es obligatorio")
    private UUID patientId;

    @NotNull(message = "La latitud es obligatoria")
    private Double latitude;

    @NotNull(message = "La longitud es obligatoria")
    private Double longitude;

    @NotNull(message = "El radio es obligatorio")
    @Min(value = 10, message = "El radio mínimo es 10 metros")
    private Integer radiusMeters;
}