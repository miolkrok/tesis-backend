package com.distribuida.dtos;

import jakarta.validation.constraints.Min;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BusquedaActividadRequest {

    // Ubicación
    private String ubicacion; // Ciudad, provincia o lugar específico
    private Double latitud;   // Para búsqueda por proximidad
    private Double longitud;  // Para búsqueda por proximidad
    private Double radioKm = 50.0; // Radio de búsqueda en km

    // Fechas
    private LocalDate fechaInicio;
    private LocalDate fechaFin;

    // Capacidad
    @Min(value = 1, message = "La cantidad de personas debe ser al menos 1")
    private Integer cantidadPersonas;

    // Filtros adicionales
    private String tipoActividad;
    private BigDecimal precioMinimo;
    private BigDecimal precioMaximo;
    private String nivelDificultad;

    // Ordenamiento y paginación
    private String ordenarPor = "fechaCreacion"; // fechaCreacion, precio, popularidad
    private String direccion = "DESC"; // ASC, DESC
    private Integer pagina = 0;
    private Integer tamanoPagina = 20;

    // Búsqueda por texto
    private String textoBusqueda;
}


