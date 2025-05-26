package com.distribuida.db;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "historial")
@Data
public class HistorialReserva {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "historial_seq")
    @SequenceGenerator(name = "historial_seq", sequenceName = "historial_seq", allocationSize=1)
    private Integer id;

    @Column(name="estado_anterior")
    private String estadoAnterior;

    @Column(name="estado_nuevo")
    private String estadoNuevo;

    @Column(name="fecha_cambio")
    private LocalDateTime fechaCambio;

    @ManyToOne
    @JoinColumn(name="reserva_id_historial")
    private Reserva reserva;



}
