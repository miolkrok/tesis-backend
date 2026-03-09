package com.distribuida.dtos;

import com.distribuida.db.Galeria;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ActividadDTO {

    private Integer id;

    private Integer proveedorId;

    @NotBlank(message = "El título es requerido")
    @Size(min = 3, max = 200, message = "El título debe tener entre 3 y 200 caracteres")
    private String titulo;

    private String descripcion;

    private String ubicacionDestino;

    private String ubicacionSalida;

    private String tipoActividad;

    private String nivelDificultad;

    @DecimalMin(value = "0.0", inclusive = true, message = "El precio debe ser mayor o igual a 0")
    private BigDecimal precio;

    private String duracion;

    //@Type(JsonType.class)
    private String disponibilidad;

    // NUEVOS CAMPOS
    private LocalDate fechaInicioDisponible;

    private LocalDate fechaFinDisponible;

    private Integer minimoPersonas;

    private Integer maximoPersonas;

    private String provincia;

    private String ciudad;

    @DecimalMin(value = "-90.0", message = "La latitud debe ser mayor o igual a -90")
    @DecimalMax(value = "90.0", message = "La latitud debe ser menor o igual a 90")
    private Double latitud;

    private Double longitud;

    private String estadoActividad;

    private String cuentaBancaria;

    private LocalDateTime fechaCreacion;

    private LocalDateTime fechaActualizacion;

    // Información del usuario (para mostrar en UI)
    private String nombreUsuario;
    private String apellidoUsuario;
    private String emailUsuario;
    private String fechaRegistroUsuario;

    private List<GaleriaDTO> galeria;

    private List<ServicioEventoDTO> servicioEvento;

}
