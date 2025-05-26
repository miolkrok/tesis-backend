package com.distribuida.db;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "actividad")
@Data
public class Actividad {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "actividad_seq")
    @SequenceGenerator(name = "actividad_seq", sequenceName = "actividad_seq", allocationSize=1)
    private Integer id;

    @Column(name = "proveedor_id")
    private Integer proveedorId;

    private String titulo;

    private String descripcion;

    private String ubicacion;

    @Column(name="tipo_actividad")
    private String tipoActividad;

    @Column(name="nivel_dificultad")
    private String nivelDificultad;

    private BigDecimal precio;

    private String duracion;

    //@Type(JsonType.class)
    private String disponibilidad;

    @Column(name="fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name="fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @OneToMany(mappedBy = "actividad", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Galeria> galeria;





}
