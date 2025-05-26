package com.distribuida.db;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "reporte_opinion")
@Data
public class ReporteOpinion {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "reporte_opinion_seq")
    @SequenceGenerator(name = "reporte_opinion_seq", sequenceName = "reporte_opinion_seq", allocationSize=1)
    private Integer id;

    @Column(name="usuario_reportador_id")
    private String usuarioReportadorId;

    @Column(name="razon")
    private String razon;

    @Column(name="estado")
    private String estado;

    @Column(name="fecha_reporte")
    private LocalDateTime fechaReporte;

    @ManyToOne
    @JoinColumn(name = "opin_id")
    private Opinion opinion;

}
