package com.distribuida.dtos;

import com.distribuida.db.HistorialReserva;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ReservaDTO {

    private Integer id;
    private Integer actividadId;
    private Integer usuarioId;
    private String estado;
    private LocalDateTime fechaReserva;
    private LocalDateTime fechaActividad;
    private Integer cantidadPersonas;
    private BigDecimal costoTotal;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    private List<HistorialReserva> historialReserva;
}
