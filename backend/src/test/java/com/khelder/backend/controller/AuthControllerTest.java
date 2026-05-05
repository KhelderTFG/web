package com.khelder.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.khelder.backend.dto.auth.AuthResponse;
import com.khelder.backend.dto.auth.LoginRequest;
import com.khelder.backend.dto.auth.RegisterRequest;
import com.khelder.backend.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.khelder.backend.config.SecurityConfig;
import com.khelder.backend.config.JwtConfig;
import org.springframework.context.annotation.Import;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtConfig.class})
@DisplayName("AuthController — Tests de capa web")
class AuthControllerTest {

    @Autowired private MockMvc      mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AuthService        authService;
    @MockBean private com.khelder.backend.security.JwtService jwtService;
    @MockBean private com.khelder.backend.security.UserDetailsServiceImpl userDetailsServiceImpl;
    @MockBean private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    private final AuthResponse authResponse = AuthResponse.builder()
            .token("jwt.token.here")
            .caregiverId(UUID.randomUUID())
            .name("Carlos Martínez")
            .email("carlos@khelder.com")
            .build();

    @Test
    @DisplayName("POST /register — petición válida devuelve 201 con token")
    void register_ValidRequest_Returns201WithToken() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setName("Carlos Martínez");
        request.setEmail("carlos@khelder.com");
        request.setPassword("password123");

        when(authService.register(any())).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt.token.here"))
                .andExpect(jsonPath("$.email").value("carlos@khelder.com"));
    }

    @Test
    @DisplayName("POST /register — email inválido devuelve 400")
    void register_InvalidEmail_Returns400() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setName("Carlos");
        request.setEmail("not-valid");
        request.setPassword("password123");

        mockMvc.perform(post("/api/v1/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /register — contraseña corta devuelve 400")
    void register_ShortPassword_Returns400() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setName("Carlos");
        request.setEmail("carlos@khelder.com");
        request.setPassword("123");

        mockMvc.perform(post("/api/v1/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /login — credenciales válidas devuelve 200 con token")
    void login_ValidCredentials_Returns200WithToken() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("carlos@khelder.com");
        request.setPassword("password123");

        when(authService.login(any())).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt.token.here"))
                .andExpect(jsonPath("$.name").value("Carlos Martínez"));
    }

    @Test
    @DisplayName("POST /login — body vacío devuelve 400")
    void login_EmptyBody_Returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }
}