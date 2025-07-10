package com.distribuida.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class BusquedaRapidaRequest {

    @NotNull(message = "La ubicación es requerida")
    private String ubicacion; // "Quito", "Baños de Agua Santa", etc.

    @NotNull(message = "La fecha de inicio es requerida")
    private LocalDate fechaInicio;

    @NotNull(message = "La fecha de fin es requerida")
    private LocalDate fechaFin;

    @NotNull(message = "La cantidad de personas es requerida")
    @Min(value = 1, message = "Debe ser al menos 1 persona")
    private Integer cantidadPersonas;

    // === CAMPOS OPCIONALES ===

    // Coordenadas (si el frontend puede obtenerlas)
    private Double latitud;
    private Double longitud;
    private Double radioKm = 25.0; // Radio por defecto 25km

    // Filtros adicionales
    private String tipoActividad; // "AVENTURA", "CULTURAL", "NATURALEZA"
    private String nivelDificultad; // "FACIL", "MODERADO", "DIFICIL"

    // Rango de precios
    private Double precioMinimo;
    private Double precioMaximo;

    // Paginación
    private Integer pagina = 0;
    private Integer tamanoPagina = 12; // 12 resultados como Booking

    // Ordenamiento
    private String ordenarPor = "RELEVANCIA"; // RELEVANCIA, PRECIO_ASC, PRECIO_DESC, DISTANCIA

    // === VALIDACIONES PERSONALIZADAS ===

    public boolean isValidDateRange() {
        if (fechaInicio == null || fechaFin == null) return false;
        return !fechaFin.isBefore(fechaInicio);
    }

    public boolean isValidPriceRange() {
        if (precioMinimo == null || precioMaximo == null) return true;
        return precioMaximo >= precioMinimo;
    }

    public long getDiasActividad() {
        if (fechaInicio == null || fechaFin == null) return 0;
        return java.time.temporal.ChronoUnit.DAYS.between(fechaInicio, fechaFin) + 1;
    }
}
