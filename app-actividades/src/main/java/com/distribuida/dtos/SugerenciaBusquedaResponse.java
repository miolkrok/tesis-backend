package com.distribuida.dtos;

import lombok.Data;

import java.util.List;

@Data
public class SugerenciaBusquedaResponse {

    private List<String> sugerenciasUbicacion;
    private List<String> sugerenciasActividad;
    private List<ActividadDTO> actividadesPopulares;
}
