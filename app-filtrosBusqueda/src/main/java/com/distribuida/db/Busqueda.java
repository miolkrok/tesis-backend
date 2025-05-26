package com.distribuida.db;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;

import java.math.BigDecimal;

@Entity
@Table(name = "busqueda")
@Data
public class Busqueda {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "busqueda_seq")
    @SequenceGenerator(name = "busqueda_seq", sequenceName = "busqueda_seq", allocationSize=1)
    private Integer id;

    @Column(name="actividad_id")
    private Integer actividadId;

    @FullTextField
    private String titulo;

    @FullTextField
    private String descripcion;

    @KeywordField
    private String ubicacion;

    @KeywordField
    private String categoria;

    private BigDecimal precio;

    @KeywordField
    private Integer capacidad;

    private String duracion;

    @Column(name="tipo_actividad")
    private String tipoActividad;

    @Column(name="nivel_dificultad")
    private String nivelDificultad;

    @Column(name = "proveedor_id")
    private Integer proveedorId;

    @KeywordField
    @Column(name = "nombre_proveedor")
    private String nombreProveedor;

    @Column(name = "puntuacion_promedio")
    private Double puntuacionPromedio;

}
