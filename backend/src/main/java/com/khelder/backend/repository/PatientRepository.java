package com.khelder.backend.repository;

import com.khelder.backend.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {

    // Buscar paciente por nombre (búsqueda parcial case-insensitive)
    List<Patient> findByFullNameContainingIgnoreCase(String fullName);

    // Obtener todos los pacientes de un cuidador específico
    @Query("""
        SELECT p FROM Patient p
        JOIN p.caregiverPatients cp
        WHERE cp.caregiver.caregiverId = :caregiverId
        """)
    List<Patient> findAllByCaregiverId(@Param("caregiverId") UUID caregiverId);

    // Verificar si un paciente está asignado a un cuidador
    @Query("""
        SELECT COUNT(cp) > 0 FROM CaregiverPatient cp
        WHERE cp.caregiver.caregiverId = :caregiverId
        AND cp.patient.patientId = :patientId
        """)
    boolean existsAssignment(
        @Param("caregiverId") UUID caregiverId,
        @Param("patientId")   UUID patientId
    );
}