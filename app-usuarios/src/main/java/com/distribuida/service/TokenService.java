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
    @ConfigProperty(name = "mp.jwt.verify.issuer")
    String issuer;

    @ConfigProperty(name = "jwt.access-token.duration")
    Duration accessTokenDuration;

    @ConfigProperty(name = "jwt.refresh-token.duration")
    Duration refreshTokenDuration;

    public String generateAccessToken(Usuario usuario) {
        String token = Jwt.issuer(issuer)
                .upn(usuario.getEmail())
                .groups(new HashSet<>(Arrays.asList(usuario.getRol().split(","))))
                .claim("userId", usuario.getId())
                .claim("nombre", usuario.getNombre())
                .claim("apellido", usuario.getApellido())
                .expiresAt(Instant.now().plus(accessTokenDuration))
                .sign();

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
}
