package com.khelder.backend.service;

import com.khelder.backend.dto.auth.AuthResponse;
import com.khelder.backend.dto.auth.LoginRequest;
import com.khelder.backend.dto.auth.RegisterRequest;
import com.khelder.backend.entity.Caregiver;
import com.khelder.backend.repository.CaregiverRepository;
import com.khelder.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final CaregiverRepository  caregiverRepository;
    private final PasswordEncoder      passwordEncoder;
    private final JwtService           jwtService;
    private final AuthenticationManager authenticationManager;

    // -------------------------------------------------------------------------
    // RF-08: Registro de cuidador
    // -------------------------------------------------------------------------

    public AuthResponse register(RegisterRequest request) {

        if (caregiverRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException(
                "Ya existe un cuidador con el email: " + request.getEmail()
            );
        }

        Caregiver caregiver = Caregiver.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .build();

        caregiverRepository.save(caregiver);

        UserDetails userDetails = buildUserDetails(caregiver);
        String token = jwtService.generateToken(userDetails);

        return AuthResponse.builder()
                .token(token)
                .caregiverId(caregiver.getCaregiverId())
                .name(caregiver.getName())
                .email(caregiver.getEmail())
                .build();
    }

    // -------------------------------------------------------------------------
    // RF-08: Login de cuidador
    // -------------------------------------------------------------------------

    public AuthResponse login(LoginRequest request) {

        // Spring Security valida credenciales automáticamente
        // y lanza excepción si son incorrectas
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        Caregiver caregiver = caregiverRepository
                .findByEmail(request.getEmail())
                .orElseThrow();

        UserDetails userDetails = buildUserDetails(caregiver);
        String token = jwtService.generateToken(userDetails);

        return AuthResponse.builder()
                .token(token)
                .caregiverId(caregiver.getCaregiverId())
                .name(caregiver.getName())
                .email(caregiver.getEmail())
                .build();
    }

    private UserDetails buildUserDetails(Caregiver caregiver) {
        return User.builder()
                .username(caregiver.getEmail())
                .password(caregiver.getPassword())
                .authorities("ROLE_CAREGIVER")
                .build();
    }
}