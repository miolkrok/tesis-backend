package com.distribuida.db;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "notificacion")
@Data
public class PreferenciasNotificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notificacion_seq")
    @SequenceGenerator(name = "notificacion_seq", sequenceName = "notificacion_seq", allocationSize=1)
    private Integer id;

    @Column(name="usuario_id")
    private String usuarioId;

    @Column(name="tipo")
    private String tipo;

    @Column(name="activo")
    private Boolean activo;



}
