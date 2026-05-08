package com.khelder.backend.security;

import com.khelder.backend.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtConfig jwtConfig;

    // -------------------------------------------------------------------------
    // Generación de tokens
    // -------------------------------------------------------------------------

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails
    ) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtConfig.getExpiration()))
                .signWith(getSigningKey())
                .compact();
    }

    // -------------------------------------------------------------------------
    // Validación de tokens
    // -------------------------------------------------------------------------

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // -------------------------------------------------------------------------
    // Extracción de claims
    // -------------------------------------------------------------------------

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtConfig.getSecret()
                .getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateDeviceToken(UserDetails userDetails) {
        return Jwts.builder()
                .claims(new HashMap<>())
                .subject(userDetails.getUsername())
                .issuedAt(new Date())
                // Sin .expiration() → token permanente
                .signWith(getSigningKey())
                .compact();
    }

    public boolean isDeviceTokenValid(String token) {
        try {
            extractUsername(token); // Si no lanza excepción, la firma es válida
            // Para tokens de dispositivo no hay expiración que comprobar
            Claims claims = extractAllClaims(token);
            return claims.getSubject() != null && 
                claims.getSubject().startsWith("device:");
        } catch (Exception e) {
            return false;
        }
    }
}