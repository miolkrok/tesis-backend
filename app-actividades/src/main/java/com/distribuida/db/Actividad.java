package com.distribuida.db;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "actividad")
@Data
@ToString(exclude = {"galeria", "servicioEvento"})
public class Actividad {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "actividad_seq")
    @SequenceGenerator(name = "actividad_seq", sequenceName = "actividad_seq", allocationSize=1)
    private Integer id;

    @Column(name = "usuario_id")
    private Integer usuarioId;

    private String titulo;

    private String descripcion;

    @Column(name="ubicacion_destino")
    private String ubicacionDestino;

    @Column(name="ubicacion_salida")
    private String ubicacionSalida;

    @Column(name="tipo_actividad")
    private String tipoActividad;

    @Column(name="nivel_dificultad")
    private String nivelDificultad;

    private BigDecimal precio;

    private String duracion;

    private String disponibilidad;

    @Column(name="cantidad_personas")
    private Integer maximoPersonas;

    @Column(name="fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name="fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @OneToMany(mappedBy = "actividad", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Galeria> galeria;

    @OneToMany(mappedBy = "actividadServicio", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ServicioEvento> servicioEvento;





}
