package com.distribuida.dtos;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class BusquedaRapidaResultDTO {

    // Información básica de la actividad
    private Integer id;
    private String titulo;
    private BigDecimal precio;

    // Rating promedio de las opiniones
    private Double rating;
    private Integer totalOpiniones;

    // Imágenes de la actividad
    private List<ImagenActividadDTO> imagenes;
    private ImagenActividadDTO imagenPrincipal;

    // Información adicional útil
    private String ubicacionDestino;
    private String tipoActividad;
    private String duracion;
    private Integer minimoPersonas;
    private Integer maximoPersonas;
    private Double distanciaKm; // Si se busca por proximidad

    @Data
    public static class ImagenActividadDTO {
        private Integer id;
        private String imagenBase64;
        private String nombreArchivo;
        private String tipoContenido;
        private Boolean esImagenPrincipal;
    }
}
