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
@Table(name = "alert",
    indexes = @Index(name = "idx_alert_device_timestamp",
                     columnList = "device_id, timestamp"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "alert_id", updatable = false, nullable = false)
    private UUID alertId;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @CreationTimestamp
    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @Column(name = "alert_type", nullable = false)
    private String alertType;

    @Column(name = "status", nullable = false)
    private String status;

    // Campos adicionales no en el diagrama pero necesarios
    // para el contexto completo de la alerta
    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "heart_rate")
    private Double heartRate;

    @Column(name = "battery_level")
    private Integer batteryLevel;
}