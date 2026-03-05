package com.distribuida.dtos;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    //@Type(JsonType.class)
    private String disponibilidad;

    // NUEVOS CAMPOS
    private LocalDate fechaInicioDisponible;

    private LocalDate fechaFinDisponible;

    private Integer minimoPersonas;

    private String provincia;

    private String ciudad;

    private Double latitud;

    private Double longitud;

    private String estadoActividad;

    private Integer maxPersonas;

    private LocalDateTime fechaCreacion;

    private LocalDateTime fechaActualizacion;

    private List<GaleriaDTO> galeria;

    private List<ServicioEventoDTO> servicioEvento;
}
