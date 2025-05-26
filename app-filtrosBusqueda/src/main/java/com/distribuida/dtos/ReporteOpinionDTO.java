package com.distribuida.dtos;

import java.time.LocalDateTime;

public class ReporteOpinionDTO {

    private Integer id;

    private String usuarioReportadorId;

    private String razon;

    private String estado;

    private LocalDateTime fechaReporte;

    private OpinionDTO opinion;
}
