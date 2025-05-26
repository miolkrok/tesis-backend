package com.distribuida.db;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "reserva")
@Data
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "reserva_seq")
    @SequenceGenerator(name = "reserva_seq", sequenceName = "reserva_seq", allocationSize=1)
    private Integer id;

    @Column(name = "actividad_id")
    private Integer actividadId;

    @Column(name = "usuario_id")
    private Integer usuarioId;

    @Column(name="estado")
    private String estado;

    @Column(name="fecha_reserva")
    private LocalDateTime fechaReserva;

    @Column(name="fecha_actividad")
    private LocalDateTime fechaActividad;

    @Column(name="cantidad_personas")
    private Integer cantidadPersonas;

    @Column(name="costo_total")
    private BigDecimal costoTotal;

    @Column(name="fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name="fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @OneToMany(mappedBy = "reserva",cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<HistorialReserva> historialReserva;

}
