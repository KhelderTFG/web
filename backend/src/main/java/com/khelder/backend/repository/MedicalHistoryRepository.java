package com.khelder.backend.repository;

import com.khelder.backend.entity.MedicalHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MedicalHistoryRepository extends JpaRepository<MedicalHistory, UUID> {

    // Obtener historial médico por paciente (relación 1:1)
    Optional<MedicalHistory> findByPatientPatientId(UUID patientId);

    // Verificar si el paciente ya tiene historial médico
    boolean existsByPatientPatientId(UUID patientId);

    void deleteByPatientPatientId(UUID patientId);

}