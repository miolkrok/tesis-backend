package com.distribuida.dtos;

import com.distribuida.db.Reembolso;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.OneToOne;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PagoDTO {

    private Integer id;

    private Integer reservaId;

    private Integer usuarioId;

    private BigDecimal monto;

    private String metodoPago;

    private String estado;

    private LocalDateTime fechaTransaccion;

    private LocalDateTime fechaActualizacion;

    private Reembolso reembolso;
}
