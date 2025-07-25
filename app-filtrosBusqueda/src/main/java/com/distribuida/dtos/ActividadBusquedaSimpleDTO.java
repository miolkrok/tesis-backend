package com.distribuida.dtos;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ActividadBusquedaSimpleDTO {

    private Integer id;
    private String titulo;
    private String imagen;
    private BigDecimal precio;
    private Double rating;

    // CAMPOS ADICIONALES ÚTILES
    private Integer totalOpiniones;
    private Double distanciaKm;

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
