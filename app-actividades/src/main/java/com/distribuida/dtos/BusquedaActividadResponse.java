package com.distribuida.dtos;

import lombok.Data;

@Data
public class BusquedaActividadResponse {

    private java.util.List<ActividadDTO> actividades;
    private Long totalElementos;
    private Integer totalPaginas;
    private Integer paginaActual;
    private Integer elementosPorPagina;
    private Boolean hayMasPaginas;

    // Metadatos de la búsqueda
    private java.util.List<String> provinciasEncontradas;
    private java.util.List<String> tiposActividadEncontrados;
    private java.util.Map<String, Object> rangosPrecios;
}
