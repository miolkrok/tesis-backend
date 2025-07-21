package com.distribuida.dtos;

import com.distribuida.db.ReporteOpinion;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class OpinionDTO {

    private Integer id;

    private Integer actividadId;

    private Integer usuarioId;

    private Integer calificacion;

    private String comentario;

    private LocalDateTime fechaCreacion;

    private LocalDateTime fechaActualizacion;

    private List<ReporteOpinionDTO> reporteOpinion;

    private String nombreUsuario;

    private String tituloActividad;
}
