package com.distribuida.dtos;

import jakarta.persistence.Column;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class UsuarioDTO {

    private Integer id;

    private String nombre;

    private String apellido;

    private String email;

    private String password;

    private String telefono;

    private String direccion;

    private String rol;

    private LocalDateTime fechaCreacion;

    private LocalDateTime fechaActualizacion;

    ProveedorDto proveedor;
}
