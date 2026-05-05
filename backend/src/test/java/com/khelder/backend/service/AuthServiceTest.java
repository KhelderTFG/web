package com.khelder.backend.service;

import com.khelder.backend.dto.auth.AuthResponse;
import com.khelder.backend.dto.auth.LoginRequest;
import com.khelder.backend.dto.auth.RegisterRequest;
import com.khelder.backend.entity.Caregiver;
import com.khelder.backend.repository.CaregiverRepository;
import com.khelder.backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService — Tests unitarios")
class AuthServiceTest {

    @Mock private CaregiverRepository   caregiverRepository;
    @Mock private PasswordEncoder       passwordEncoder;
    @Mock private JwtService            jwtService;
    @Mock private AuthenticationManager authenticationManager;

    @InjectMocks private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest    loginRequest;
    private Caregiver       caregiver;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setName("Carlos Martínez");
        registerRequest.setEmail("carlos@khelder.com");
        registerRequest.setPassword("password123");
        registerRequest.setPhone("600111222");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("carlos@khelder.com");
        loginRequest.setPassword("password123");

        caregiver = Caregiver.builder()
                .caregiverId(UUID.randomUUID())
                .name("Carlos Martínez")
                .email("carlos@khelder.com")
                .password("$2a$10$hashedpassword")
                .phone("600111222")
                .build();
    }

    // -------------------------------------------------------------------------
    // RF-08: Registro
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("RF-08: Registro exitoso devuelve token JWT")
    void register_Success_ReturnsToken() {
        when(caregiverRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashed");
        when(caregiverRepository.save(any())).thenReturn(caregiver);
        when(jwtService.generateToken(any())).thenReturn("jwt.token.here");

        AuthResponse response = authService.register(registerRequest);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt.token.here");
        assertThat(response.getEmail()).isEqualTo("carlos@khelder.com");
        verify(caregiverRepository).save(any(Caregiver.class));
    }

    @Test
    @DisplayName("RF-08: Registro con email duplicado lanza excepción")
    void register_DuplicateEmail_ThrowsException() {
        when(caregiverRepository.existsByEmail("carlos@khelder.com"))
                .thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("carlos@khelder.com");
    }

    // -------------------------------------------------------------------------
    // RF-08: Login
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("RF-08: Login exitoso devuelve token JWT")
    void login_Success_ReturnsToken() {
        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(caregiverRepository.findByEmail("carlos@khelder.com"))
                .thenReturn(Optional.of(caregiver));
        when(jwtService.generateToken(any())).thenReturn("jwt.token.here");

        AuthResponse response = authService.login(loginRequest);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt.token.here");
        assertThat(response.getName()).isEqualTo("Carlos Martínez");
    }

    @Test
    @DisplayName("RF-08: Login con credenciales incorrectas lanza excepción")
    void login_BadCredentials_ThrowsException() {
        when(authenticationManager.authenticate(
                any(UsernamePasswordAuthenticationToken.class))
        ).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class);
    }
}