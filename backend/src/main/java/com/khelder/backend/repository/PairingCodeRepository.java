package com.khelder.backend.repository;

import com.khelder.backend.entity.PairingCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PairingCodeRepository extends JpaRepository<PairingCode, String> {
    Optional<PairingCode> findByCodeAndApprovedFalseAndExpiresAtAfter(
        String code, LocalDateTime now
    );
    Optional<PairingCode> findByCodeAndApprovedTrueAndExpiresAtAfter(
        String code, LocalDateTime now
    );
    void deleteByDeviceId(String deviceId);
}