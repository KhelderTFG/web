package com.khelder.backend.dto.report;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ReportRequest {

    @NotNull(message = "El patient_id es obligatorio")
    private UUID patientId;

    @NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDateTime from;

    @NotNull(message = "La fecha de fin es obligatoria")
    private LocalDateTime to;

    // "pdf" o "csv"
    private String format = "pdf";
}