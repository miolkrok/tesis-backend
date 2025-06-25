package com.distribuida.db;

import io.quarkus.security.jpa.Password;
import io.quarkus.security.jpa.Roles;
import io.quarkus.security.jpa.UserDefinition;
import io.quarkus.security.jpa.Username;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;


@Entity
@Table(name = "usuario")
@Data
@ToString(exclude = "proveedor")
@UserDefinition
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "usuario_seq")
    @SequenceGenerator(name = "usuario_seq", sequenceName = "usuario_seq", allocationSize=1)
    @Column(name = "id", updatable = false, nullable = false)
    private Integer id;

    private String nombre;

    private String apellido;

    @Username
    @Column(unique = true, nullable = false)
    private String email;

    @Password
    private String password;

    private String telefono;

    private String direccion;

    @Roles
    private String rol;

    private String imagenPerfil;

    @Column(name="activo")
    private Boolean activo = true;

    @Column(name="email_verificado")
    private Boolean emailVerificado = false;

    @Column(name="fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name="fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "proveedor_id_usuario")
    private Proveedor proveedor;

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RefreshToken> refreshTokens;

    public void setProveedor(Proveedor proveedor) {
        this.proveedor = proveedor;
        if (proveedor != null) {
            proveedor.setUsuario(this);
        }
    }
}
