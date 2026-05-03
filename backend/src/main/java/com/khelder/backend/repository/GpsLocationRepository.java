package com.khelder.backend.repository;

import com.khelder.backend.entity.GpsLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GpsLocationRepository extends JpaRepository<GpsLocation, UUID> {

    // Última ubicación conocida de un dispositivo
    Optional<GpsLocation> findFirstByDeviceIdOrderByTimestampDesc(String deviceId);

    /**
     * Comprueba si el punto está dentro del radio de una zona segura.
     * Usa ST_DWithin de PostGIS para cálculo espacial eficiente.
     * ST_DWithin en geography trabaja en metros directamente.
     *
     * Usado por RF-14: Zonas Seguras
     */
    @Query(value = """
        SELECT COUNT(*) > 0
        FROM gps_location gl, safe_zone sz
        WHERE gl.location_id = :locationId
        AND sz.zone_id = :zoneId
        AND ST_DWithin(
            gl.coordinates::geography,
            sz.centroid::geography,
            sz.radius_meters
        )
        """, nativeQuery = true)
    boolean isWithinSafeZone(
        @Param("locationId") UUID locationId,
        @Param("zoneId")     UUID zoneId
    );
}