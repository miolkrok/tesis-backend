package com.distribuida.dtos;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ActividadDTO {

    private Integer id;

    private Integer proveedorId;

    private String titulo;

    private String descripcion;

    private String ubicacionDestino;

    private String ubicacionSalida;

    private String tipoActividad;

    private String nivelDificultad;

    private BigDecimal precio;

    private String duracion;

    private String disponibilidad;

    private LocalDateTime fechaCreacion;

    private LocalDateTime fechaActualizacion;

    private List<GaleriaDTO> galeria;

    private List<ServicioEventoDTO> servicioEvento;
}
