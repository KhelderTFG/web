package com.khelder.backend.service;

import com.khelder.backend.dto.reminder.ReminderRequest;
import com.khelder.backend.dto.reminder.ReminderResponse;
import com.khelder.backend.entity.Caregiver;
import com.khelder.backend.entity.Patient;
import com.khelder.backend.entity.Reminder;
import com.khelder.backend.repository.CaregiverRepository;
import com.khelder.backend.repository.PatientRepository;
import com.khelder.backend.repository.ReminderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@DisplayName("ReminderService — Tests unitarios")
class ReminderServiceTest {

    @Mock private ReminderRepository  reminderRepository;
    @Mock private PatientRepository   patientRepository;
    @Mock private CaregiverRepository caregiverRepository;

    @InjectMocks private ReminderService reminderService;

    private Caregiver caregiver;
    private Patient   patient;
    private Reminder  reminder;
    private UUID      caregiverId;
    private UUID      patientId;
    private UUID      reminderId;

    @BeforeEach
    void setUp() {
        caregiverId = UUID.randomUUID();
        patientId   = UUID.randomUUID();
        reminderId  = UUID.randomUUID();

        caregiver = Caregiver.builder()
                .caregiverId(caregiverId)
                .email("carlos@khelder.com")
                .name("Carlos Martínez")
                .build();

        patient = Patient.builder()
                .patientId(patientId)
                .fullName("María Antonia García")
                .build();

        reminder = Reminder.builder()
                .reminderId(reminderId)
                .patient(patient)
                .caregiver(caregiver)
                .message("Tomar Enalapril 10mg")
                .scheduledDate(LocalDateTime.now().plusHours(1))
                .status("PENDING")
                .build();

        // Simular cuidador autenticado
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("carlos@khelder.com");
        SecurityContext secCtx = mock(SecurityContext.class);
        when(secCtx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(secCtx);

        when(caregiverRepository.findByEmail("carlos@khelder.com"))
                .thenReturn(Optional.of(caregiver));
    }

    // -------------------------------------------------------------------------
    // RF-10: Crear recordatorio
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("RF-10: Crear recordatorio válido lo guarda correctamente")
    void createReminder_Valid_SavesReminder() {
        ReminderRequest request = new ReminderRequest();
        request.setPatientId(patientId);
        request.setMessage("Tomar Enalapril 10mg");
        request.setScheduledDate(LocalDateTime.now().plusHours(2));

        when(patientRepository.existsAssignment(caregiverId, patientId))
                .thenReturn(true);
        when(patientRepository.findById(patientId))
                .thenReturn(Optional.of(patient));
        when(caregiverRepository.findById(caregiverId))
                .thenReturn(Optional.of(caregiver));
        when(reminderRepository.save(any())).thenReturn(reminder);

        ReminderResponse response = reminderService.createReminder(request);

        assertThat(response).isNotNull();
        assertThat(response.getMessage()).isEqualTo("Tomar Enalapril 10mg");
        assertThat(response.getStatus()).isEqualTo("PENDING");
        verify(reminderRepository).save(any(Reminder.class));
    }

    @Test
    @DisplayName("RF-10: Crear recordatorio sin permisos lanza SecurityException")
    void createReminder_NoPermission_ThrowsSecurityException() {
        ReminderRequest request = new ReminderRequest();
        request.setPatientId(patientId);
        request.setMessage("Tomar medicación");
        request.setScheduledDate(LocalDateTime.now().plusHours(1));

        when(patientRepository.existsAssignment(caregiverId, patientId))
                .thenReturn(false);

        assertThatThrownBy(() -> reminderService.createReminder(request))
                .isInstanceOf(SecurityException.class);
    }

    // -------------------------------------------------------------------------
    // RF-10: Listar recordatorios
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("RF-10: Listar recordatorios de un paciente")
    void getRemindersByPatient_ReturnsReminderList() {
        when(patientRepository.existsAssignment(caregiverId, patientId))
                .thenReturn(true);
        when(reminderRepository.findByPatientPatientIdOrderByScheduledDateAsc(patientId))
                .thenReturn(List.of(reminder));

        List<ReminderResponse> result =
                reminderService.getRemindersByPatient(patientId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMessage()).isEqualTo("Tomar Enalapril 10mg");
    }

    @Test
    @DisplayName("RF-10: Listar recordatorios sin permisos lanza SecurityException")
    void getRemindersByPatient_NoPermission_ThrowsSecurityException() {
        when(patientRepository.existsAssignment(caregiverId, patientId))
                .thenReturn(false);

        assertThatThrownBy(() -> reminderService.getRemindersByPatient(patientId))
                .isInstanceOf(SecurityException.class);
    }

    // -------------------------------------------------------------------------
    // RF-10: Actualizar recordatorio
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("RF-10: Actualizar recordatorio PENDING lo modifica correctamente")
    void updateReminder_Pending_UpdatesSuccessfully() {
        ReminderRequest request = new ReminderRequest();
        request.setPatientId(patientId);
        request.setMessage("Tomar Enalapril 20mg (dosis actualizada)");
        request.setScheduledDate(LocalDateTime.now().plusHours(3));

        when(reminderRepository.findById(reminderId))
                .thenReturn(Optional.of(reminder));
        when(reminderRepository.save(any())).thenReturn(reminder);

        ReminderResponse response =
                reminderService.updateReminder(reminderId, request);

        assertThat(response).isNotNull();
        verify(reminderRepository).save(reminder);
    }

    @Test
    @DisplayName("RF-10: Actualizar recordatorio CONFIRMED lanza IllegalStateException")
    void updateReminder_Confirmed_ThrowsIllegalStateException() {
        reminder.setStatus("CONFIRMED");

        ReminderRequest request = new ReminderRequest();
        request.setPatientId(patientId);
        request.setMessage("Nuevo mensaje");
        request.setScheduledDate(LocalDateTime.now().plusHours(1));

        when(reminderRepository.findById(reminderId))
                .thenReturn(Optional.of(reminder));

        assertThatThrownBy(() -> reminderService.updateReminder(reminderId, request))
                .isInstanceOf(IllegalStateException.class);
    }

    // -------------------------------------------------------------------------
    // RF-10: Eliminar recordatorio
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("RF-10: Eliminar recordatorio propio funciona correctamente")
    void deleteReminder_OwnedReminder_DeletesSuccessfully() {
        when(reminderRepository.findById(reminderId))
                .thenReturn(Optional.of(reminder));

        reminderService.deleteReminder(reminderId);

        verify(reminderRepository).deleteById(reminderId);
    }

    @Test
    @DisplayName("RF-10: Eliminar recordatorio ajeno lanza SecurityException")
    void deleteReminder_NotOwned_ThrowsSecurityException() {
        Caregiver otherCaregiver = Caregiver.builder()
                .caregiverId(UUID.randomUUID())
                .build();

        Reminder otherReminder = Reminder.builder()
                .reminderId(reminderId)
                .caregiver(otherCaregiver)
                .patient(patient)
                .status("PENDING")
                .build();

        when(reminderRepository.findById(reminderId))
                .thenReturn(Optional.of(otherReminder));

        assertThatThrownBy(() -> reminderService.deleteReminder(reminderId))
                .isInstanceOf(SecurityException.class);
    }

    // -------------------------------------------------------------------------
    // RF-06: Confirmar recordatorio
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("RF-06: Confirmar recordatorio cambia estado a CONFIRMED")
    void confirmReminder_UpdatesStatusToConfirmed() {
        var request = new com.khelder.backend.dto.reminder.ReminderConfirmationRequest();
        request.setReminderId(reminderId.toString());
        request.setDeviceId("watch-maria-001");

        when(reminderRepository.findById(reminderId))
                .thenReturn(Optional.of(reminder));
        when(reminderRepository.save(any())).thenReturn(reminder);

        reminderService.confirmReminder(request);

        assertThat(reminder.getStatus()).isEqualTo("CONFIRMED");
        verify(reminderRepository).save(reminder);
    }
}