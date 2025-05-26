package com.distribuida.dtos;

import com.distribuida.db.Opinion;
import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.time.LocalDateTime;

public class ReporteOpinionDTO {

    private Integer id;

    private String usuarioReportadorId;

    private String razon;

    private String estado;

    private LocalDateTime fechaReporte;

    private OpinionDTO opinion;
}
