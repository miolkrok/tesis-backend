package com.distribuida.dtos;

import lombok.Data;

@Data
public class GaleriaDTO {
    private Integer id;
    private Integer actividadId;
    private String urlFoto;
    private String imagenBinaria;
    private String nombreArchivo;
    private String tipoContenido;
    private Long tamanoArchivo;
    private Boolean esImagenPrincipal;
}