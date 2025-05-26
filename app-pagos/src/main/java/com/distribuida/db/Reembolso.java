package com.distribuida.db;


import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "reembolso")
@Data
public class Reembolso {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "reembolso_seq")
    @SequenceGenerator(name = "reembolso_seq", sequenceName = "reembolso_seq", allocationSize=1)
    private Integer id;

    @Column(name="monto")
    private BigDecimal monto;

    @Column(name="fecha_reembolso")
    private LocalDateTime fechaReembolso;

    @Column(name="estado")
    private String estado;

    @OneToOne(cascade = CascadeType.ALL)
    private Pago pago;

}
