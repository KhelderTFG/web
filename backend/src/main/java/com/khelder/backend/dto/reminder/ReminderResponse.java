package com.khelder.backend.dto.reminder;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReminderResponse {
    private UUID          reminderId;
    private UUID          patientId;
    private String        patientName;
    private UUID          caregiverId;
    private String        message;
    private LocalDateTime scheduledDate;
    private String        status;
    private LocalDateTime confirmedAt;
}