package com.khelder.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "gps_location",
    indexes = @Index(name = "idx_gps_timestamp",
    columnList = "device_id, timestamp"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GpsLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "location_id", updatable = false, nullable = false)
    private UUID locationId;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    /**
     * Tipo espacial PostGIS — almacena latitud y longitud como
     * un punto geográfico optimizado para consultas de distancia.
     */
    @Column(name = "coordinates", columnDefinition = "geometry(Point,4326)")
    private Point coordinates;

    @CreationTimestamp
    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;
}