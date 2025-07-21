package com.distribuida.dtos;

import lombok.Data;

@Data
public class GaleriaDTO {

    private Integer id;
    private String urlFoto;
    private Integer actividadId;

    private String imagenBinaria; // Para enviar/recibir imagen en Base64
    private String nombreArchivo;
    private String tipoContenido;
    private Long tamanoArchivo;
    private Boolean esImagenPrincipal;
}
