package com.distribuida.dtos;


import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

}
