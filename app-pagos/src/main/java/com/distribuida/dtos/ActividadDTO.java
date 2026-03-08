package com.distribuida.dtos;


import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ActividadDTO {

    private Integer id;

    private Integer proveedorId;  // Este es el usuarioId (anfitrión)
    private Integer usuarioId;

    private String titulo;
    private String descripcion;
    private String ubicacionDestino;
    private String ubicacionSalida;
    private String tipoActividad;
    private String nivelDificultad;
    private BigDecimal precio;
    private String duracion;
    private String disponibilidad;
    private LocalDate fechaInicioDisponible;
    private LocalDate fechaFinDisponible;
    private Integer minimoPersonas;
    private Integer maximoPersonas;
    private String provincia;
    private String ciudad;
    private Double latitud;
    private Double longitud;
    private String estadoActividad;
    private String cuentaBancaria;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;

    private List<GaleriaDTO> galeria;
    private List<ServicioEventoDTO> servicioEvento;

    // Información del usuario (para mostrar en UI)
    private String nombreUsuario;
    private String apellidoUsuario;
    private String emailUsuario;
    private String fechaRegistroUsuario;

    // Información del proveedor
    private String nombreProveedor;
    private String descripcionProveedor;


}