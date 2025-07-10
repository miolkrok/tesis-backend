package com.distribuida.dtos;

import lombok.Data;

import java.util.List;

@Data
public class ActividadBusquedaDTO {

    // Información básica
    private Integer id;
    private String titulo;
    private String descripcion;
    private String imagenPrincipal;

    // Ubicación
    private String ubicacionDestino;
    private String provincia;
    private String ciudad;
    private Double distanciaKm; // Distancia desde punto de búsqueda

    // Precio y disponibilidad
    private Double precio;
    private Double precioTotal; // precio * cantidadPersonas * dias
    private Boolean disponible;
    private String motivoNoDisponible; // Si no está disponible

    // Características
    private String tipoActividad;
    private String nivelDificultad;
    private String duracion;
    private Integer minimoPersonas;
    private Integer maximoPersonas;

    // Calificación y popularidad
    private Double puntuacionPromedio;
    private Integer numeroResenias;
    private Integer numeroReservas;

    // Proveedor
    private String nombreProveedor;
    private Double calificacionProveedor;

    // Coordenadas (para mapa)
    private Double latitud;
    private Double longitud;

    // Etiquetas útiles
    private List<String> etiquetas; // ["Familia", "Aventura", "Cancelación gratis"]
    private Boolean cancelacionGratuita;
    private Boolean confirmacionInmediata;
}
