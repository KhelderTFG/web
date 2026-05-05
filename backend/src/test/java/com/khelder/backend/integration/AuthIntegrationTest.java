package com.khelder.backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.khelder.backend.dto.auth.LoginRequest;
import com.khelder.backend.dto.auth.RegisterRequest;
import com.khelder.backend.repository.CaregiverRepository;
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

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Disabled("Requiere PostgreSQL con PostGIS — ejecutar manualmente")
@DisplayName("Auth — Tests de integración")
class AuthIntegrationTest {

    @Autowired private MockMvc         mockMvc;
    @Autowired private ObjectMapper    objectMapper;
    @Autowired private CaregiverRepository caregiverRepository;

    @BeforeEach
    void setUp() {
        caregiverRepository.deleteAll();
    }

    @Test
    @DisplayName("RF-08: Registro y login completo devuelve token válido")
    void registerAndLogin_Success_ReturnsValidToken() throws Exception {

        // 1. Registro
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setName("Test Caregiver");
        registerRequest.setEmail("test@khelder.com");
        registerRequest.setPassword("password123");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.email").value("test@khelder.com"));

        // 2. Login con las mismas credenciales
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@khelder.com");
        loginRequest.setPassword("password123");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.name").value("Test Caregiver"));
    }

    @Test
    @DisplayName("RF-08: Login con contraseña incorrecta devuelve 403")
    void login_WrongPassword_Returns403() throws Exception {

        // Registrar primero
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setName("Test Caregiver");
        registerRequest.setEmail("test2@khelder.com");
        registerRequest.setPassword("password123");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // Login con contraseña incorrecta
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test2@khelder.com");
        loginRequest.setPassword("wrongpassword");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RF-08: Registro con email inválido devuelve 400")
    void register_InvalidEmail_Returns400() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setName("Test");
        request.setEmail("not-an-email");
        request.setPassword("password123");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Endpoint protegido sin token devuelve 403")
    void protectedEndpoint_WithoutToken_Returns403() throws Exception {
        mockMvc.perform(post("/api/v1/patients"))
                .andExpect(status().isForbidden());
    }
}