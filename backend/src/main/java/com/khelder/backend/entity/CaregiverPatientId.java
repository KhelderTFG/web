package com.khelder.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaregiverPatientId implements Serializable {

    @Column(name = "caregiver_id")
    private UUID caregiverId;

    @Column(name = "patient_id")
    private UUID patientId;
}