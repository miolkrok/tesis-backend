package com.distribuida.db;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "galeria")
@Data
public class Galeria {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "galeria_seq")
    @SequenceGenerator(name = "galeria_seq", sequenceName = "galeria_seq", allocationSize=1)
    private Integer id;

    @Column(name="url_foto")
    private String urlFoto;

    @ManyToOne
    @JoinColumn(name = "acti_id")
    private Actividad actividad;
}
