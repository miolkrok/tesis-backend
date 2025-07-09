package com.distribuida.dtos;

import jakarta.validation.constraints.NotNull;

public class BusquedaRapidaRequest {
    @NotNull(message = "El texto de búsqueda es requerido")
    private String texto;

    private Integer limite = 10;
}
