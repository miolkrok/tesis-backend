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

    @Lob
    @Column(name = "imagen_binaria")
    private byte[] imagenBinaria;

    @Column(name = "nombre_archivo")
    private String nombreArchivo;

    @Column(name = "tipo_contenido")
    private String tipoContenido;

    @Column(name = "tamano_archivo")
    private Long tamanoArchivo;

    @Column(name = "es_imagen_principal")
    private Boolean esImagenPrincipal = false;

    @ManyToOne
    @JoinColumn(name = "acti_id")
    private Actividad actividad;
}
