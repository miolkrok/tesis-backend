package com.distribuida.dtos;

import lombok.Data;

@Data
public class PagoDTO {

    private Integer id;
    private Integer reservaId;
    private String estado;  // PENDIENTE, APROBADO, RECHAZADO
}
