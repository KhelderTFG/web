package com.khelder.backend.repository;

import com.khelder.backend.entity.CaregiverPatient;
import com.khelder.backend.entity.CaregiverPatientId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CaregiverPatientRepository
        extends JpaRepository<CaregiverPatient, CaregiverPatientId> {

    // Todos los pacientes asignados a un cuidador
    List<CaregiverPatient> findByIdCaregiverId(UUID caregiverId);

    // Todos los cuidadores asignados a un paciente
    List<CaregiverPatient> findByIdPatientId(UUID patientId);

    // Eliminar asignación específica
    void deleteByIdCaregiverIdAndIdPatientId(UUID caregiverId, UUID patientId);

    // Verificar si existe la asignación
    boolean existsByIdCaregiverIdAndIdPatientId(UUID caregiverId, UUID patientId);

    void deleteByIdPatientId(UUID patientId);

}