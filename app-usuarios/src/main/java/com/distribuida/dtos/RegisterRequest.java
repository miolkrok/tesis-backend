package com.distribuida.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {


    @NotBlank(message = "El nombre es requerido")
    @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
    private String nombre;

    @NotBlank(message = "El apellido es requerido")
    @Size(min = 2, max = 50, message = "El apellido debe tener entre 2 y 50 caracteres")
    private String apellido;

    @NotBlank(message = "El email es requerido")
    @Email(message = "El email debe ser válido")
    private String email;

    @NotBlank(message = "La contraseña es requerida")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=]).*$",
            message = "La contraseña debe contener al menos un número, una letra minúscula, una letra mayúscula y un carácter especial")
    private String password;

    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "El teléfono debe ser válido")
    private String telefono;

    private String direccion;

    @NotBlank(message = "El rol es requerido")
    @Pattern(regexp = "CLIENTE|PROVEEDOR|ADMIN", message = "El rol debe ser CLIENTE, PROVEEDOR o ADMIN")
    private String rol;

    private ProveedorDto proveedor;
}
