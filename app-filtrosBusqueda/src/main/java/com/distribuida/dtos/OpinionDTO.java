package com.distribuida.dtos;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class OpinionDTO {

    private Integer id;

    private String actividadId;

    private String usuarioId;

    private Integer calificacion;

    private String comentario;

    private LocalDateTime fechaCreacion;

    private LocalDateTime fechaActualizacion;

    private List<ReporteOpinionDTO> reporteOpinion;
}
