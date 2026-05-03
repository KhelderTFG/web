package com.khelder.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "caregiver")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Caregiver {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "caregiver_id", updatable = false, nullable = false)
    private UUID caregiverId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "phone")
    private String phone;

    // Contraseña hasheada — no incluida en el diagrama pero necesaria para RF-08
    @Column(name = "password", nullable = false)
    private String password;

    // Relación N:M con Patient a través de CaregiverPatient
    @OneToMany(mappedBy = "caregiver", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CaregiverPatient> caregiverPatients;

    // Recordatorios creados por este cuidador
    @OneToMany(mappedBy = "caregiver", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Reminder> reminders;

    // Zonas seguras configuradas por este cuidador
    @OneToMany(mappedBy = "caregiver", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SafeZone> safeZones;
}