package com.khelder.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "pairing_code")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PairingCode {

    @Id
    @Column(name = "code", length = 6)
    private String code;

    @Column(name = "device_id", nullable = false)
    private String deviceId;

    @Column(name = "approved", nullable = false)
    private Boolean approved = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
}