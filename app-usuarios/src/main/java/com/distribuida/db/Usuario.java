package com.distribuida.db;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;


@Entity
@Table(name = "usuario")
@Data
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "usuario_seq")
    @SequenceGenerator(name = "usuario_seq", sequenceName = "usuario_seq", allocationSize=1)
    @Column(name = "id", updatable = false, nullable = false)
    private Integer id;

    @Column(name="nombre")
    private String nombre;

    @Column(name="apellido")
    private String apellido;

    @Column(name="email")
    private String email;

    @Column(name="password")
    private String password;

    @Column(name="telefono")
    private String telefono;

    @Column(name="direccion")
    private String direccion;

    @Column(name="rol")
    private String rol;

    @Column(name="fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name="fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "proveedor_id_usuario")
    Proveedor proveedor;
}
