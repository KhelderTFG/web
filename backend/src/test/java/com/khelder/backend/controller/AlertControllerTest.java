package com.khelder.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.khelder.backend.dto.alert.AlertRequest;
import com.khelder.backend.dto.alert.AlertResponse;
import com.khelder.backend.service.AlertService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AlertController.class)
@DisplayName("AlertController — Tests de capa web")
class AlertControllerTest {

    @Autowired private MockMvc      mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AlertService       alertService;
    @MockBean private com.khelder.backend.security.JwtService jwtService;
    @MockBean private com.khelder.backend.security.UserDetailsServiceImpl userDetailsServiceImpl;
    @MockBean private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    private final UUID alertId = UUID.randomUUID();

    private final AlertResponse alertResponse = AlertResponse.builder()
            .alertId(alertId)
            .deviceId("watch-maria-001")
            .patientName("María Antonia García")
            .alertType("SOS_MANUAL")
            .status("ACTIVE")
            .latitude(37.3891)
            .longitude(-5.9845)
            .timestamp(LocalDateTime.now())
            .build();

    @Test
    @WithMockUser
    @DisplayName("POST /alerts — alerta SOS válida devuelve 201")
    void createAlert_ValidSOS_Returns201() throws Exception {
        AlertRequest request = new AlertRequest();
        request.setDeviceId("watch-maria-001");
        request.setAlertType("SOS_MANUAL");
        request.setLatitude(37.3891);
        request.setLongitude(-5.9845);
        request.setTimestamp(System.currentTimeMillis());

        when(alertService.saveAlert(any())).thenReturn(alertResponse);

        mockMvc.perform(post("/api/v1/alerts")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.alert_type").value("SOS_MANUAL"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /alerts — sin device_id devuelve 400")
    void createAlert_MissingDeviceId_Returns400() throws Exception {
        AlertRequest request = new AlertRequest();
        request.setAlertType("SOS_MANUAL");
        request.setTimestamp(System.currentTimeMillis());

        mockMvc.perform(post("/api/v1/alerts")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /alerts — devuelve página de alertas activas")
    void getMyAlerts_Returns200WithPage() throws Exception {
        when(alertService.getMyAlerts(anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(alertResponse)));

        mockMvc.perform(get("/api/v1/alerts")
                .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].alert_type")
                        .value("SOS_MANUAL"));
    }

    @Test
    @WithMockUser
    @DisplayName("PUT /alerts/{id}/resolve — resuelve alerta correctamente")
    void resolveAlert_Returns200() throws Exception {
        AlertResponse resolved = AlertResponse.builder()
                .alertId(alertId)
                .alertType("SOS_MANUAL")
                .status("RESOLVED")
                .timestamp(LocalDateTime.now())
                .build();

        when(alertService.resolveAlert(any(), any())).thenReturn(resolved);

        mockMvc.perform(put("/api/v1/alerts/{id}/resolve", alertId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"observations\":\"Falsa alarma\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));
    }

    @Test
    @WithMockUser
    @DisplayName("PUT /alerts/{id}/cancel — cancela alerta correctamente")
    void cancelAlert_Returns200() throws Exception {
        AlertResponse cancelled = AlertResponse.builder()
                .alertId(alertId)
                .alertType("SOS_MANUAL")
                .status("CANCELLED")
                .timestamp(LocalDateTime.now())
                .build();

        when(alertService.cancelAlert(any())).thenReturn(cancelled);

        mockMvc.perform(put("/api/v1/alerts/{id}/cancel", alertId)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}