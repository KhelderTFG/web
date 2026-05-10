package com.khelder.backend.repository;

import com.khelder.backend.entity.SafeZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SafeZoneRepository extends JpaRepository<SafeZone, UUID> {

    // Zonas seguras de un paciente — RF-14
    List<SafeZone> findByPatientPatientId(UUID patientId);

    // Zonas seguras configuradas por un cuidador
    List<SafeZone> findByCaregiverCaregiverId(UUID caregiverId);

    /**
     * Verifica si unas coordenadas están dentro de alguna
     * zona segura del paciente. Usa PostGIS ST_DWithin.
     * Devuelve las zonas que contienen el punto dado.
     *
     * RF-14: Alertas de Zonas Seguras
     */
    @Query(value = """
        SELECT sz.* FROM safe_zone sz
        WHERE sz.patient_id = :patientId
        AND ST_DWithin(
            sz.centroid::geography,
            ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
            sz.radius_meters
        )
        """, nativeQuery = true)
    List<SafeZone> findZonesContainingPoint(
        @Param("patientId")  UUID patientId,
        @Param("latitude")   double latitude,
        @Param("longitude")  double longitude
    );

    /**
     * Verifica si el paciente está FUERA de todas sus zonas seguras.
     * Devuelve true si el punto no está en ninguna zona.
     * Usado para generar la alerta GEOFENCE_EXIT.
     */
    @Query(value = """
        SELECT COUNT(*) = 0 FROM safe_zone sz
        WHERE sz.patient_id = :patientId
        AND ST_DWithin(
            sz.centroid::geography,
            ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
            sz.radius_meters
        )
        """, nativeQuery = true)
    boolean isOutsideAllSafeZones(
        @Param("patientId")  UUID patientId,
        @Param("latitude")   double latitude,
        @Param("longitude")  double longitude
    );

    void deleteByPatientPatientId(UUID patientId);
}