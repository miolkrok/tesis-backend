package com.distribuida.dtos;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class BusquedaActividadDTO {

    // Campos principales para sincronización
    private Integer actividadId;
    private String titulo;
    private String descripcion;
    private String ubicacion;
    private String categoria;
    private BigDecimal precio;
    private String duracion;
    private String tipoActividad;
    private String nivelDificultad;

    // Información del proveedor
    private Integer proveedorId;
    private String nombreProveedor;

    // Ubicación geográfica
    private String provincia;
    private String ciudad;
    private Double latitud;
    private Double longitud;

    // Disponibilidad
    private LocalDate fechaInicioDisponible;
    private LocalDate fechaFinDisponible;
    private Integer minimoPersonas;
    private Integer maximoPersonas;
    private String estadoActividad;

    // Metadatos
    private LocalDateTime fechaIndexacion;
    private Double puntuacionPromedio;
    private Integer numeroReservas;
    private Integer numeroOpiniones;

    // Constructor vacío
    public BusquedaActividadDTO() {}

    // Constructor desde ActividadDTO
    public BusquedaActividadDTO(ActividadDTO actividad) {
        this.actividadId = actividad.getId();
        this.titulo = actividad.getTitulo();
        this.descripcion = actividad.getDescripcion();
        this.ubicacion = actividad.getUbicacionDestino();
        this.categoria = actividad.getTipoActividad();
        this.precio = actividad.getPrecio();
        this.duracion = actividad.getDuracion();
        this.tipoActividad = actividad.getTipoActividad();
        this.nivelDificultad = actividad.getNivelDificultad();
        this.proveedorId = actividad.getProveedorId();
        this.provincia = actividad.getProvincia();
        this.ciudad = actividad.getCiudad();
        this.latitud = actividad.getLatitud();
        this.longitud = actividad.getLongitud();
        this.fechaInicioDisponible = actividad.getFechaInicioDisponible();
        this.fechaFinDisponible = actividad.getFechaFinDisponible();
        this.minimoPersonas = actividad.getMinimoPersonas();
        this.maximoPersonas = actividad.getMaximoPersonas();
        this.estadoActividad = actividad.getEstadoActividad();
        this.fechaIndexacion = LocalDateTime.now();

        // Valores por defecto para nuevas actividades
        this.puntuacionPromedio = 0.0;
        this.numeroReservas = 0;
        this.numeroOpiniones = 0;
    }

}
