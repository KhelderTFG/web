package com.khelder.backend.controller;

import com.khelder.backend.repository.GpsLocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/gps")
@RequiredArgsConstructor
public class GpsLocationController {

    private final GpsLocationRepository gpsLocationRepository;

    /**
     * Última ubicación GPS de un dispositivo
     * GET /api/v1/gps/device/{deviceId}/latest
     */
    @GetMapping("/device/{deviceId}/latest")
    public ResponseEntity<Map<String, Double>> getLatest(
            @PathVariable String deviceId
    ) {
        return gpsLocationRepository
                .findFirstByDeviceIdOrderByTimestampDesc(deviceId)
                .map(loc -> ResponseEntity.ok(Map.of(
                        "latitude",  loc.getCoordinates().getY(),
                        "longitude", loc.getCoordinates().getX()
                )))
                .orElse(ResponseEntity.notFound().build());
    }
}