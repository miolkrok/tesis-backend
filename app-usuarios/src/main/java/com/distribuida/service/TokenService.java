package com.distribuida.service;

import com.distribuida.db.Usuario;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;


@ApplicationScoped
public class TokenService {
    /**
     * Servicio para generar tokens JWT
     * @author Distribuida
     */
    @ConfigProperty(name = "mp.jwt.verify.issuer")
    String issuer;

    @ConfigProperty(name = "jwt.access-token.duration")
    Duration accessTokenDuration;

    @ConfigProperty(name = "jwt.refresh-token.duration")
    Duration refreshTokenDuration;

    public String generateAccessToken(Usuario usuario) {
        Instant now = Instant.now();

        String token = Jwt.issuer(issuer)
                .upn(usuario.getEmail())
                .groups(new HashSet<>(Arrays.asList(usuario.getRol().split(","))))
                .claim("userId", usuario.getId())  //  Integer directo
                .claim("nombre", usuario.getNombre())
                .claim("apellido", usuario.getApellido())
                .claim("rol", usuario.getRol())
                .claim("email", usuario.getEmail())
                .claim("iat", now.getEpochSecond())
                .claim("exp", now.plus(accessTokenDuration).getEpochSecond())
                .claim("aud", "microservices")  // Audiencia para todos los microservicios
                .expiresAt(now.plus(accessTokenDuration))
                .sign();

        System.out.println("Token JWT generado para usuario: " + usuario.getId() +
                " | Rol: " + usuario.getRol() +
                " | Expira en: " + accessTokenDuration.toMinutes() + " minutos");

        return token;
    }

    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }

    public Duration getAccessTokenDuration() {
        return accessTokenDuration;
    }

    public Duration getRefreshTokenDuration() {
        return refreshTokenDuration;
    }

    public String getIssuer() {
        return issuer;
    }
}
