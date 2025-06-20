package com.distribuida.db;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

@Entity
@Table(name = "proveedor")
@Data
@ToString(exclude = "usuario")
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "proveedor_seq")
    @SequenceGenerator(name = "proveedor_seq", sequenceName = "proveedor_seq", allocationSize=1)
    private Integer id;

    @Column(name="nombre_empresa")
    private String nombreEmpresa;

    @Column(name="descripcion_empresa")
    private String descripcionEmpresa;

    @Column(name="metodo_pago")
    private String metodoPago;

    @OneToOne(mappedBy = "proveedor",cascade = CascadeType.ALL,fetch = FetchType.LAZY)
    Usuario usuario;

}
