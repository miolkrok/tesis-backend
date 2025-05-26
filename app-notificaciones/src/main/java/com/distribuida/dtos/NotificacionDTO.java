package com.distribuida.dtos;

import jakarta.persistence.Column;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificacionDTO {

    private Integer id;

    private Integer usuarioId;

    private String tipo;

    private String mensaje;

    private String tipoEntidad; // "RESERVA", "ACTIVIDAD", "PAGO", etc.

    private String estado; // "PENDIENTE", "ENVIADA", "LEIDA", "ERROR"

    private LocalDateTime fechaCreacion;

    private LocalDateTime fechaEnvio;

    private String emailUsuario;

    private String titulo;

    private LocalDateTime fechaEvento;
}
