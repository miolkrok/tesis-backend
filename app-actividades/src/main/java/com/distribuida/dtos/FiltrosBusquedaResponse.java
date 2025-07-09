package com.distribuida.dtos;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class FiltrosBusquedaResponse {

    private List<String> provincias;
    private List<String> tiposActividad;
    private List<String> nivelesificultad;
    private Map<String, Object> rangosPrecios;
    private Long totalActividades;
}
