package com.distribuida.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "El email es requerido")
    @Email(message = "El email debe ser válido")
    private String email;

    @NotBlank(message = "La contraseña es requerida")
    private String password;

    private String userAgent;
    private String ipAddress;
}
