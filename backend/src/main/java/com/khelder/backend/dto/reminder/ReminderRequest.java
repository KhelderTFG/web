package com.khelder.backend.dto.reminder;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ReminderRequest {

    @NotNull(message = "El patient_id es obligatorio")
    private UUID patientId;

    @NotBlank(message = "El mensaje es obligatorio")
    private String message;

    @NotNull(message = "La fecha programada es obligatoria")
    @Future(message = "La fecha debe ser en el futuro")
    private LocalDateTime scheduledDate;
}