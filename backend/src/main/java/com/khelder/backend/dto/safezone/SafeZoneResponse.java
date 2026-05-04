package com.khelder.backend.dto.safezone;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SafeZoneResponse {
    private UUID    zoneId;
    private UUID    patientId;
    private String  patientName;
    private UUID    caregiverId;
    private Double  latitude;
    private Double  longitude;
    private Integer radiusMeters;
}