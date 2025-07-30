package com.distribuida.dtos;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ActividadBusquedaSimpleDTO {

    private Integer id;
    private String titulo;
    private String imagen; // Base64 de la imagen principal
    private BigDecimal precio;
    private Double rating; // Promedio de calificaciones (1-5)

    // Campos adicionales útiles
    private Integer totalOpiniones;
    private Double distanciaKm; // Si se busca por proximidad
    private String ubicacionDestino;
    private String tipoActividad;

    // Constructor vacío
    public ActividadBusquedaSimpleDTO() {}

    // Constructor con campos principales
    public ActividadBusquedaSimpleDTO(Integer id, String titulo, String imagen,
                                      BigDecimal precio, Double rating) {
        this.id = id;
        this.titulo = titulo;
        this.imagen = imagen;
        this.precio = precio;
        this.rating = rating;
    }
}
