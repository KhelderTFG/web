package com.khelder.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;

import java.util.UUID;

@Entity
@Table(name = "safe_zone")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SafeZone {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "zone_id", updatable = false, nullable = false)
    private UUID zoneId;

    // Relación N:1 con Patient
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    // Relación N:1 con Caregiver (quien la configura)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caregiver_id", nullable = false)
    private Caregiver caregiver;

    /**
     * Centro de la zona segura como tipo espacial PostGIS.
     * SRID 4326 = sistema de coordenadas WGS84 (GPS estándar).
     * Permite calcular distancias con ST_Distance de PostGIS.
     */
    @Column(name = "centroid", columnDefinition = "geometry(Point,4326)")
    private Point centroid;

    /**
     * Radio en metros desde el centroide.
     * La zona segura es un círculo definido por centroide + radio.
     */
    @Column(name = "radius_meters", nullable = false)
    private Integer radiusMeters;
}