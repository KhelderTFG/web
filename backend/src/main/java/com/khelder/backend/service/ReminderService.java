package com.khelder.backend.service;

import com.khelder.backend.dto.reminder.ReminderConfirmationRequest;
import com.khelder.backend.dto.reminder.ReminderRequest;
import com.khelder.backend.dto.reminder.ReminderResponse;
import com.khelder.backend.entity.Caregiver;
import com.khelder.backend.entity.Patient;
import com.khelder.backend.entity.Reminder;
import com.khelder.backend.repository.CaregiverRepository;
import com.khelder.backend.repository.PatientRepository;
import com.khelder.backend.repository.ReminderRepository;
import com.khelder.backend.repository.SmartwatchRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderService {

    private final ReminderRepository  reminderRepository;
    private final PatientRepository   patientRepository;
    private final CaregiverRepository caregiverRepository;
    private final FcmService         fcmService;
    private final SmartwatchRepository smartwatchRepository;

    // -------------------------------------------------------------------------
    // RF-10: Crear recordatorio
    // -------------------------------------------------------------------------

    @Transactional
    public ReminderResponse createReminder(ReminderRequest request) {
        UUID caregiverId = getAuthenticatedCaregiverId();

        // Verificar que el cuidador tiene acceso al paciente
        if (!patientRepository.existsAssignment(caregiverId, request.getPatientId())) {
            throw new SecurityException(
                "No tienes permisos para crear recordatorios para este paciente"
            );
        }

        Patient  patient  = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new IllegalArgumentException(
                    "Paciente no encontrado: " + request.getPatientId()
                ));

        Caregiver caregiver = caregiverRepository.findById(caregiverId)
                .orElseThrow();

        Reminder reminder = Reminder.builder()
                .patient(patient)
                .caregiver(caregiver)
                .message(request.getMessage())
                .scheduledDate(request.getScheduledDate())
                .status("PENDING")
                .build();

        Reminder saved = reminderRepository.save(reminder);

        log.info("Recordatorio creado: {} para paciente {}",
                saved.getReminderId(), request.getPatientId());

        // Buscar el smartwatch del paciente y enviar via FCM
        smartwatchRepository.findByPatientPatientId(request.getPatientId())
                .ifPresent(sw -> {
                    if (sw.getFcmToken() != null) {
                        long scheduledAtMs = saved.getScheduledDate()
                                .atZone(java.time.ZoneId.systemDefault())
                                .toInstant()
                                .toEpochMilli();
                        fcmService.sendReminderToDevice(
                                sw.getFcmToken(),
                                saved.getReminderId().toString(),
                                saved.getMessage(),
                                "",
                                scheduledAtMs
                        );
                    }
                });

        return toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // RF-10: Listar recordatorios de un paciente
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ReminderResponse> getRemindersByPatient(UUID patientId) {
        UUID caregiverId = getAuthenticatedCaregiverId();

        if (!patientRepository.existsAssignment(caregiverId, patientId)) {
            throw new SecurityException(
                "No tienes permisos para ver los recordatorios de este paciente"
            );
        }

        return reminderRepository
                .findByPatientPatientIdOrderByScheduledDateAsc(patientId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // -------------------------------------------------------------------------
    // RF-10: Obtener recordatorios de todos mis pacientes
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ReminderResponse> getMyReminders() {
        UUID caregiverId = getAuthenticatedCaregiverId();
        return reminderRepository
                .findByCaregiverCaregiverIdOrderByScheduledDateAsc(caregiverId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // -------------------------------------------------------------------------
    // RF-10: Actualizar recordatorio
    // -------------------------------------------------------------------------

    @Transactional
    public ReminderResponse updateReminder(
            UUID reminderId,
            ReminderRequest request
    ) {
        UUID caregiverId = getAuthenticatedCaregiverId();

        Reminder reminder = reminderRepository.findById(reminderId)
                .orElseThrow(() -> new IllegalArgumentException(
                    "Recordatorio no encontrado: " + reminderId
                ));

        if (!reminder.getCaregiver().getCaregiverId().equals(caregiverId)) {
            throw new SecurityException(
                "No tienes permisos para modificar este recordatorio"
            );
        }

        if (!reminder.getStatus().equals("PENDING")) {
            throw new IllegalStateException(
                "Solo se pueden modificar recordatorios en estado PENDING"
            );
        }

        reminder.setMessage(request.getMessage());
        reminder.setScheduledDate(request.getScheduledDate());
        reminderRepository.save(reminder);

        return toResponse(reminder);
    }

    // -------------------------------------------------------------------------
    // RF-10: Eliminar recordatorio
    // -------------------------------------------------------------------------

    @Transactional
    public void deleteReminder(UUID reminderId) {
        UUID caregiverId = getAuthenticatedCaregiverId();

        Reminder reminder = reminderRepository.findById(reminderId)
                .orElseThrow(() -> new IllegalArgumentException(
                    "Recordatorio no encontrado: " + reminderId
                ));

        if (!reminder.getCaregiver().getCaregiverId().equals(caregiverId)) {
            throw new SecurityException(
                "No tienes permisos para eliminar este recordatorio"
            );
        }

        reminderRepository.deleteById(reminderId);
        log.info("Recordatorio eliminado: {}", reminderId);
    }

    // -------------------------------------------------------------------------
    // RF-06: Recordatorios pendientes para un dispositivo
    // Consultado por el móvil al conectarse
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ReminderResponse> getPendingByDevice(String deviceId) {
        return reminderRepository
                .findPendingByDeviceId(deviceId, LocalDateTime.now())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // -------------------------------------------------------------------------
    // RF-06: Confirmar recordatorio desde el reloj
    // -------------------------------------------------------------------------

    @Transactional
    public void confirmReminder(ReminderConfirmationRequest request) {
        UUID reminderId = UUID.fromString(request.getReminderId());

        Reminder reminder = reminderRepository.findById(reminderId)
                .orElseThrow(() -> new IllegalArgumentException(
                    "Recordatorio no encontrado: " + reminderId
                ));

        reminder.setStatus("CONFIRMED");
        reminderRepository.save(reminder);

        log.info("Recordatorio confirmado: {}", reminderId);

        // TODO Issue #32: notificar al panel web via WebSocket
    }

    // -------------------------------------------------------------------------
    // Utilidades
    // -------------------------------------------------------------------------

    private UUID getAuthenticatedCaregiverId() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
        return caregiverRepository.findByEmail(email)
                .orElseThrow()
                .getCaregiverId();
    }

    private ReminderResponse toResponse(Reminder r) {
        return ReminderResponse.builder()
                .reminderId(r.getReminderId())
                .patientId(r.getPatient().getPatientId())
                .patientName(r.getPatient().getFullName())
                .caregiverId(r.getCaregiver().getCaregiverId())
                .message(r.getMessage())
                .scheduledDate(r.getScheduledDate())
                .status(r.getStatus())
                .build();
    }
}