package com.distribuida.dtos;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class BusquedaRapidaResponse {

    //RESULTADOS PRINCIPALES
    private List<ActividadBusquedaDTO> actividades;

    //INFORMACIÓN DE PAGINACIÓN
    private Integer totalElementos;
    private Integer totalPaginas;
    private Integer paginaActual;
    private Integer elementosPorPagina;
    private Boolean hayMasPaginas;

    //  METADATOS DE LA BUSQUEDA
    private String ubicacionBuscada;
    private String fechaInicio;
    private String fechaFin;
    private Integer cantidadPersonas;
    private Long diasActividad;

    // FILTROS DISPONIBLES (para refinar búsqueda) ===
    private List<String> tiposActividadDisponibles;
    private List<String> provinciasCercanas;
    private Map<String, Object> rangosPrecios;

    // Estadisticas de la búsqueda
    private Integer actividadesEncontradas;
    private Double precioPromedio;
    private String ubicacionMasCercana;
    private Double distanciaPromedio; // en km

    // SUGERENCIAS
    private List<String> sugerenciasUbicacion;
    private List<String> sugerenciasFechas;
}
