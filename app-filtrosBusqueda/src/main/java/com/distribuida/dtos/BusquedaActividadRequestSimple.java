package com.distribuida.dtos;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BusquedaActividadRequestSimple {

    private String ubicacion;
    private Double latitud;
    private Double longitud;
    private Double radioKm = 50.0;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private Integer cantidadPersonas;
    private String tipoActividad;
    private BigDecimal precioMinimo;
    private BigDecimal precioMaximo;
    private String textoBusqueda;
    private Integer pagina = 0;
    private Integer tamanoPagina = 20;
}
