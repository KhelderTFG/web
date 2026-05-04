package com.khelder.backend.dto.reminder;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReminderConfirmationRequest {

    @NotBlank(message = "El reminder_id es obligatorio")
    private String reminderId;

    private String deviceId;
    private Long   confirmedAt;
}