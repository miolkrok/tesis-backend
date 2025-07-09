package com.distribuida.dtos;

import com.distribuida.db.Busqueda;
import lombok.Data;

import java.util.List;

@Data
public class BusquedaActividadResponseSimple {
    private List<Busqueda> actividades;
    private Long totalElementos;
    private Integer totalPaginas;
    private Integer paginaActual;
    private Integer elementosPorPagina;
    private Boolean hayMasPaginas;
}
