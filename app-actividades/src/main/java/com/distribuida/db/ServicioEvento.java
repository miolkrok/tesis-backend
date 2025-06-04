package com.distribuida.db;


import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "servicio_evento")
@Data
public class ServicioEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "servicio_seq")
    @SequenceGenerator(name = "servicio_seq", sequenceName = "servicio_seq", allocationSize=1)
    private Integer id;

    @Column(name="lista_servicio")
    private String listaServicio;

    @ManyToOne
    @JoinColumn(name = "acti_id_servicio")
    private Actividad actividadServicio;
}
