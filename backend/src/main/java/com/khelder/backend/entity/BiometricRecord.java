package com.khelder.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "biometric_record",
    indexes = @Index(name = "idx_biometric_timestamp",
                     columnList = "device_id, timestamp"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BiometricRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "record_id", updatable = false, nullable = false)
    private UUID recordId;

    // FK a Smartwatch
    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @Column(name = "heart_rate")
    private Double heartRate;

    @Column(name = "spo2")
    private Double spO2;

    @Column(name = "steps")
    private Long steps;

    @Column(name = "temperature")
    private Double temperature;

    @CreationTimestamp
    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;
}