package com.khelder.backend.security;

import com.khelder.backend.repository.CaregiverRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final CaregiverRepository caregiverRepository;

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        // JWT de dispositivo móvil — subject = "device:nodeId"
        if (username.startsWith("device:")) {
            return new User(
                    username,
                    "",
                    List.of(new SimpleGrantedAuthority("ROLE_DEVICE"))
            );
        }

        // JWT de cuidador — subject = email
        var caregiver = caregiverRepository
                .findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Cuidador no encontrado: " + username
                ));

        return new User(
                caregiver.getEmail(),
                caregiver.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_CAREGIVER"))
        );
    }
}