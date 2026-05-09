package com.khelder.backend.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class FcmService {

    public void sendReminderToDevice(
            String fcmToken,
            String reminderId,
            String message,
            String medicationName,
            long   scheduledAt
    ) {
        if (fcmToken == null || fcmToken.isBlank()) {
            log.warn("FCM token vacío — no se puede enviar el recordatorio");
            return;
        }

        if (FirebaseApp.getApps().isEmpty()) {
            log.warn("Firebase no inicializado — FCM desactivado");
            return;
        }

        try {
            Message fcmMessage = Message.builder()
                    .setToken(fcmToken)
                    .putAllData(Map.of(
                            "type",            "REMINDER",
                            "reminder_id",     reminderId,
                            "message",         message,
                            "medication_name", medicationName,
                            "scheduled_at",    String.valueOf(scheduledAt)
                    ))
                    .build();

            String response = FirebaseMessaging.getInstance().send(fcmMessage);
            log.info("FCM enviado correctamente: {}", response);

        } catch (Exception e) {
            log.error("Error enviando FCM: {}", e.getMessage());
        }
    }
}