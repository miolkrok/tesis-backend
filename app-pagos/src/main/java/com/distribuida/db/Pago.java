package com.distribuida.db;


import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pago")
@Data
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pago_seq")
    @SequenceGenerator(name = "pago_seq", sequenceName = "pago_seq", allocationSize=1)
    private Integer id;

    @Column(name="reserva_id")
    private Integer reservaId;

    @Column(name="usuario_id")
    private Integer usuarioId;

    private BigDecimal monto;

    private String metodoPago;

    private String estado;

    private LocalDateTime fechaTransaccion;

    private LocalDateTime fechaActualizacion;

    @OneToOne(cascade = CascadeType.ALL)
    private Reembolso reembolso;

}
