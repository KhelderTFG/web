package com.khelder.backend.repository;

import com.khelder.backend.entity.Caregiver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CaregiverRepository extends JpaRepository<Caregiver, UUID> {

    // Buscar por email — usado en autenticación JWT (RF-08)
    Optional<Caregiver> findByEmail(String email);

    // Verificar si el email ya está registrado
    boolean existsByEmail(String email);
}