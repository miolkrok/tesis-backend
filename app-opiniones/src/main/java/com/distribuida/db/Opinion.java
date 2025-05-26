package com.distribuida.db;


import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "opinion")
@Data
public class Opinion {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "opinion_seq")
    @SequenceGenerator(name = "opinion_seq", sequenceName = "opinion_seq", allocationSize=1)
    private Integer id;

    @Column(name="actividad_id")
    private String actividadId;

    @Column(name="usuario_id")
    private String usuarioId;

    @Column(name="calificacion")
    private Integer calificacion;

    @Column(name="comentario")
    private String comentario;

    @Column(name="fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name="fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @OneToMany(mappedBy = "opinion", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ReporteOpinion> reporteOpinion;
}
