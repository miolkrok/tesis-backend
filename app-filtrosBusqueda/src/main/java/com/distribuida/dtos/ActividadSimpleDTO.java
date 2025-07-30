package com.distribuida.dtos;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ActividadSimpleDTO {

    private Integer id;
    private String titulo;
    private String imagen;  // Base64 de la imagen principal
    private BigDecimal precio;
    private Double rating;   // Promedio de calificaciones (1-5)

    // Constructor vacío
    public ActividadSimpleDTO() {}

    // Constructor con todos los campos
    public ActividadSimpleDTO(Integer id, String titulo, String imagen,
                              BigDecimal precio, Double rating) {
        this.id = id;
        this.titulo = titulo;
        this.imagen = imagen;
        this.precio = precio;
        this.rating = rating;
    }
}
