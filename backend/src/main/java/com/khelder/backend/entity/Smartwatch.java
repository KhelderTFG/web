package com.khelder.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "smartwatch")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Smartwatch {

    // PK es varchar según el diagrama (device_id del reloj)
    @Id
    @Column(name = "device_id", nullable = false)
    private String deviceId;

    // Relación N:1 con Patient
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "battery_level")
    private Integer batteryLevel;

    @Column(name = "connection_status", nullable = false)
    private Boolean connectionStatus = false;

    @Column(name = "last_ping")
    private LocalDateTime lastPing;

    @Column(name = "node_id", unique = true)
    private String nodeId;

    // Registros biométricos generados por este dispositivo
    @OneToMany(mappedBy = "deviceId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BiometricRecord> biometricRecords;

    // Alertas generadas por este dispositivo
    @OneToMany(mappedBy = "deviceId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Alert> alerts;

    // Ubicaciones GPS registradas por este dispositivo
    @OneToMany(mappedBy = "deviceId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<GpsLocation> gpsLocations;
}