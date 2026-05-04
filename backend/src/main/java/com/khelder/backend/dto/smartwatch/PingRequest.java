package com.khelder.backend.dto.smartwatch;

import lombok.Data;

@Data
public class PingRequest {
    private Integer batteryLevel;
    private Double  latitude;
    private Double  longitude;
}