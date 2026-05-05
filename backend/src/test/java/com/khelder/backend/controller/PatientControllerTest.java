package com.khelder.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.khelder.backend.dto.patient.PatientDetailResponse;
import com.khelder.backend.dto.patient.PatientRequest;
import com.khelder.backend.dto.patient.PatientResponse;
import com.khelder.backend.service.PatientService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PatientController.class)
@DisplayName("PatientController — Tests de capa web")
class PatientControllerTest {

    @Autowired private MockMvc      mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private PatientService     patientService;
    @MockBean private com.khelder.backend.security.JwtService jwtService;
    @MockBean private com.khelder.backend.security.UserDetailsServiceImpl userDetailsServiceImpl;
    @MockBean private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    private final UUID patientId = UUID.randomUUID();

    private final PatientResponse patientResponse = PatientResponse.builder()
            .patientId(patientId)
            .fullName("María Antonia García")
            .dateOfBirth(LocalDate.of(1945, 3, 15))
            .age(81)
            .deviceConnected(true)
            .batteryLevel(78)
            .activeAlertsCount(2L)
            .build();

    private final PatientDetailResponse detailResponse = PatientDetailResponse.builder()
            .patientId(patientId)
            .fullName("María Antonia García")
            .dateOfBirth(LocalDate.of(1945, 3, 15))
            .age(81)
            .deviceConnected(true)
            .build();

    @Test
    @WithMockUser
    @DisplayName("GET /patients — devuelve lista de pacientes")
    void getMyPatients_Returns200WithList() throws Exception {
        when(patientService.getMyPatients()).thenReturn(List.of(patientResponse));

        mockMvc.perform(get("/api/v1/patients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].full_name")
                        .value("María Antonia García"))
                .andExpect(jsonPath("$[0].battery_level").value(78));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /patients — sin pacientes devuelve lista vacía")
    void getMyPatients_EmptyList_Returns200() throws Exception {
        when(patientService.getMyPatients()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/patients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /patients/{id} — devuelve detalle del paciente")
    void getPatientDetail_Returns200WithDetail() throws Exception {
        when(patientService.getPatientDetail(patientId))
                .thenReturn(detailResponse);

        mockMvc.perform(get("/api/v1/patients/{id}", patientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.full_name").value("María Antonia García"))
                .andExpect(jsonPath("$.age").value(81));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /patients — petición válida devuelve 201")
    void createPatient_ValidRequest_Returns201() throws Exception {
        PatientRequest request = new PatientRequest();
        request.setFullName("Antonio Ruiz");
        request.setDateOfBirth(LocalDate.of(1940, 5, 20));

        when(patientService.createPatient(any())).thenReturn(detailResponse);

        mockMvc.perform(post("/api/v1/patients")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser
    @DisplayName("POST /patients — nombre vacío devuelve 400")
    void createPatient_EmptyName_Returns400() throws Exception {
        PatientRequest request = new PatientRequest();
        request.setFullName("");
        request.setDateOfBirth(LocalDate.of(1940, 5, 20));

        mockMvc.perform(post("/api/v1/patients")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}