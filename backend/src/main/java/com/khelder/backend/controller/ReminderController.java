package com.khelder.backend.controller;

import com.khelder.backend.dto.reminder.ReminderConfirmationRequest;
import com.khelder.backend.dto.reminder.ReminderRequest;
import com.khelder.backend.dto.reminder.ReminderResponse;
import com.khelder.backend.service.ReminderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reminders")
@RequiredArgsConstructor
public class ReminderController {

    private final ReminderService reminderService;

    /**
     * RF-10: Crear recordatorio
     * POST /api/v1/reminders
     */
    @PostMapping
    public ResponseEntity<ReminderResponse> createReminder(
            @Valid @RequestBody ReminderRequest request
    ) {
        ReminderResponse response = reminderService.createReminder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * RF-10: Recordatorios de un paciente
     * GET /api/v1/reminders/patient/{patientId}
     */
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<ReminderResponse>> getRemindersByPatient(
            @PathVariable UUID patientId
    ) {
        return ResponseEntity.ok(
                reminderService.getRemindersByPatient(patientId)
        );
    }

    /**
     * RF-10: Actualizar recordatorio
     * PUT /api/v1/reminders/{reminderId}
     */
    @PutMapping("/{reminderId}")
    public ResponseEntity<ReminderResponse> updateReminder(
            @PathVariable UUID reminderId,
            @Valid @RequestBody ReminderRequest request
    ) {
        return ResponseEntity.ok(
                reminderService.updateReminder(reminderId, request)
        );
    }

    /**
     * RF-10: Eliminar recordatorio
     * DELETE /api/v1/reminders/{reminderId}
     */
    @DeleteMapping("/{reminderId}")
    public ResponseEntity<Void> deleteReminder(
            @PathVariable UUID reminderId
    ) {
        reminderService.deleteReminder(reminderId);
        return ResponseEntity.noContent().build();
    }

    /**
     * RF-06: Recordatorios pendientes para un dispositivo
     * Consultado por la app móvil
     * GET /api/v1/reminders/pending/{deviceId}
     */
    @GetMapping("/pending/{deviceId}")
    public ResponseEntity<List<ReminderResponse>> getPendingByDevice(
            @PathVariable String deviceId
    ) {
        return ResponseEntity.ok(
                reminderService.getPendingByDevice(deviceId)
        );
    }

    /**
     * RF-06: Confirmar recordatorio desde el reloj
     * POST /api/v1/reminders/confirm
     */
    @PostMapping("/confirm")
    public ResponseEntity<Void> confirmReminder(
            @Valid @RequestBody ReminderConfirmationRequest request
    ) {
        reminderService.confirmReminder(request);
        return ResponseEntity.ok().build();
    }
}