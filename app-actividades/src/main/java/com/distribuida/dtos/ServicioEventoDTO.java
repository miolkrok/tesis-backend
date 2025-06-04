package com.distribuida.dtos;

import lombok.Data;

@Data
public class ServicioEventoDTO {

    private Integer id;
    private String urlFoto;
    private ActividadDTO actividadEvento;
}
