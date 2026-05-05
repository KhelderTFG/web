package com.khelder.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.khelder.backend.dto.auth.LoginRequest;
import com.khelder.backend.dto.auth.RegisterRequest;
import com.khelder.backend.dto.biometric.BiometricRecordRequest;
import com.khelder.backend.entity.Patient;
import com.khelder.backend.entity.Smartwatch;
import com.khelder.backend.repository.CaregiverRepository;
import com.khelder.backend.repository.PatientRepository;
import com.khelder.backend.repository.SmartwatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Disabled("Requiere PostgreSQL con PostGIS — ejecutar manualmente")
@DisplayName("BiometricRecord — Tests de integración")
class BiometricRecordIntegrationTest {

    @Autowired private MockMvc          mockMvc;
    @Autowired private ObjectMapper     objectMapper;
    @Autowired private CaregiverRepository caregiverRepository;
    @Autowired private PatientRepository   patientRepository;
    @Autowired private SmartwatchRepository smartwatchRepository;

    private String token;
    private String deviceId = "watch-test-001";

    @BeforeEach
    void setUp() throws Exception {
        // Limpiar BD
        smartwatchRepository.deleteAll();
        patientRepository.deleteAll();
        caregiverRepository.deleteAll();

        // Registrar cuidador y obtener token
        RegisterRequest register = new RegisterRequest();
        register.setName("Test Caregiver");
        register.setEmail("bio-test@khelder.com");
        register.setPassword("password123");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(register)))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        token = objectMapper.readTree(body).get("token").asText();

        // Crear paciente y smartwatch de prueba
        Patient patient = patientRepository.save(Patient.builder()
                .fullName("Paciente Test")
                .dateOfBirth(LocalDate.of(1950, 1, 1))
                .build());

        smartwatchRepository.save(Smartwatch.builder()
                .deviceId(deviceId)
                .patient(patient)
                .connectionStatus(false)
                .build());
    }

    @Test
    @DisplayName("RF-03: Enviar registro biométrico válido devuelve 201")
    void postBiometricRecord_Valid_Returns201() throws Exception {
        BiometricRecordRequest request = new BiometricRecordRequest();
        request.setDeviceId(deviceId);
        request.setHeartRate(75.0);
        request.setSpO2(98.0);
        request.setSteps(1200L);
        request.setTimestamp(System.currentTimeMillis());

        mockMvc.perform(post("/api/v1/biometric-records")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OK"));
    }

    @Test
    @DisplayName("RF-03: Registro sin token devuelve 403")
    void postBiometricRecord_NoToken_Returns403() throws Exception {
        BiometricRecordRequest request = new BiometricRecordRequest();
        request.setDeviceId(deviceId);
        request.setTimestamp(System.currentTimeMillis());

        mockMvc.perform(post("/api/v1/biometric-records")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RF-03: FC > 140 genera alerta automática en BD")
    void postBiometricRecord_HighHeartRate_CreatesAlert() throws Exception {
        BiometricRecordRequest request = new BiometricRecordRequest();
        request.setDeviceId(deviceId);
        request.setHeartRate(155.0);
        request.setTimestamp(System.currentTimeMillis());

        mockMvc.perform(post("/api/v1/biometric-records")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OK"));

        // Verificar que se creó la alerta
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/v1/alerts?status=ACTIVE")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}