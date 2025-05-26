package com.distribuida.dtos;

import lombok.Data;

@Data
public class GaleriaDTO {

    private Integer id;
    private String urlFoto;
    private ActividadDTO actividad;
}
