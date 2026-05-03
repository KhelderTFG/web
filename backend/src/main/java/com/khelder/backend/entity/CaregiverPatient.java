package com.khelder.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "caregiver_patient")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaregiverPatient {

    @EmbeddedId
    private CaregiverPatientId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("caregiverId")
    @JoinColumn(name = "caregiver_id")
    private Caregiver caregiver;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("patientId")
    @JoinColumn(name = "patient_id")
    private Patient patient;
}