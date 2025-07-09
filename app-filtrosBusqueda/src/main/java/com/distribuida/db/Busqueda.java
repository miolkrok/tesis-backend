package com.distribuida.db;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "busqueda")
@Data
public class Busqueda {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "busqueda_seq")
    @SequenceGenerator(name = "busqueda_seq", sequenceName = "busqueda_seq", allocationSize=1)
    private Integer id;

    @Column(name="actividad_id")
    private Integer actividadId;

    private String titulo;

    private String descripcion;

    private String ubicacion;

    private String categoria;

    private BigDecimal precio;

    private Integer capacidad;

    private String duracion;

    @Column(name="tipo_actividad")
    private String tipoActividad;

    @Column(name="nivel_dificultad")
    private String nivelDificultad;

    @Column(name = "proveedor_id")
    private Integer proveedorId;

    @Column(name = "nombre_proveedor")
    private String nombreProveedor;

    @Column(name = "puntuacion_promedio")
    private Double puntuacionPromedio;

    @Column(name = "provincia")
    private String provincia;

    @Column(name = "ciudad")
    private String ciudad;

    @Column(name = "fecha_inicio_disponible")
    private LocalDate fechaInicioDisponible;

    @Column(name = "fecha_fin_disponible")
    private LocalDate fechaFinDisponible;

    @Column(name = "minimo_personas")
    private Integer minimoPersonas;

    @Column(name = "maximo_personas")
    private Integer maximoPersonas;

    @Column(name = "coordenada_lat")
    private Double latitud;

    @Column(name = "coordenada_lng")
    private Double longitud;

    @Column(name = "estado_actividad")
    private String estadoActividad;

    @Column(name = "fecha_indexacion")
    private LocalDateTime fechaIndexacion;

    @Column(name = "numero_reservas", columnDefinition = "integer default 0")
    private Integer numeroReservas = 0;

    @Column(name = "numero_opiniones", columnDefinition = "integer default 0")
    private Integer numeroOpiniones = 0;

}
