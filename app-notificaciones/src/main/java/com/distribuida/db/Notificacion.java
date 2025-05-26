package com.distribuida.db;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "notificacion")
@Data
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notificacion_seq")
    @SequenceGenerator(name = "notificacion_seq", sequenceName = "notificacion_seq", allocationSize=1)
    private Integer id;

    @Column(name="usuario_id")
    private Integer usuarioId;

    @Column(name = "titulo")
    private String titulo;

    @Column(name="tipo")
    private String tipo;

    @Column(name="mensaje")
    private String mensaje;

    @Column(name = "tipo_entidad")
    private String tipoEntidad; // "RESERVA", "ACTIVIDAD", "PAGO", etc.

    @Column(name = "estado")
    private String estado; // "PENDIENTE", "ENVIADA", "LEIDA", "ERROR"

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name="fecha_envio")
    private LocalDateTime fechaEnvio;


}
