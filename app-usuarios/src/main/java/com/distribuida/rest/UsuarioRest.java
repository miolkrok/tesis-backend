package com.distribuida.rest;

import com.distribuida.db.Usuario;
import com.distribuida.repo.UsuarioRepository;
import com.distribuida.service.AzureBlobStorageService;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Path("/usuarios")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transactional
@Authenticated
public class UsuarioRest {

    @Inject
    private UsuarioRepository usuarioRepo;

    @Inject
    private AzureBlobStorageService storageService;

    @Inject
    JsonWebToken jwt;

    @GET
    @RolesAllowed({"ADMIN"})
    public Response  findAll(@Context SecurityContext securityContext) {
        try {
            System.out.println("findAll usuarios - Admin access");
            System.out.println("Usuario: " + jwt.getName());
            System.out.println("Roles: " + jwt.getGroups());

            // Verificar que realmente tenga rol ADMIN
            if (!securityContext.isUserInRole("ADMIN")) {
                System.err.println("Usuario sin permisos de ADMIN intentó acceder a findAll");
                return Response.status(Response.Status.FORBIDDEN)
                        .entity(Map.of("error", "Se requieren permisos de administrador"))
                        .build();
            }

            List<Usuario> usuarios = usuarioRepo.listAll();
            System.out.println("Usuarios encontrados: " + usuarios.size());

            for (Usuario u : usuarios) {
                System.out.println("Usuario: " + u.getId() + " - " + u.getEmail() + " - " + u.getRol());
            }

            return Response.ok(usuarios).build();

        } catch (Exception e) {
            System.err.println("Error en findAll usuarios: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                            "error", "Error interno del servidor",
                            "details", e.getMessage(),
                            "timestamp", LocalDateTime.now().toString()
                    ))
                    .build();
        }
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "CLIENTE", "PROVEEDOR"})
    public Response findById(@PathParam("id") Integer id,
                             @Context SecurityContext securityContext) {

        try {
            System.out.println(" Buscando usuario ID: " + id);

            // Los usuarios solo pueden ver su propia información, excepto ADMIN
            if (!securityContext.isUserInRole("ADMIN")) {
                try {
                    // Obtener el userId manejando el tipo JsonNumber
                    Object userIdClaim = jwt.getClaim("userId");
                    Integer tokenUserId = null;

                    if (userIdClaim != null) {
                        if (userIdClaim instanceof Number) {
                            tokenUserId = ((Number) userIdClaim).intValue();
                        } else if (userIdClaim instanceof String) {
                            tokenUserId = Integer.valueOf((String) userIdClaim);
                        } else {
                            tokenUserId = Integer.valueOf(userIdClaim.toString());
                        }
                    }

                    System.out.println(" Token UserId: " + tokenUserId + " | Requested ID: " + id);

                    if (tokenUserId == null) {
                        System.err.println(" No se pudo obtener userId del JWT");
                        return Response.status(Response.Status.UNAUTHORIZED)
                                .entity("Token JWT inválido")
                                .build();
                    }

                    if (!tokenUserId.equals(id)) {
                        System.out.println(" Acceso denegado: usuario " + tokenUserId + " intentó acceder a " + id);
                        return Response.status(Response.Status.FORBIDDEN)
                                .entity("No tienes permiso para ver este usuario")
                                .build();
                    }
                } catch (Exception e) {
                    System.err.println(" Error procesando JWT: " + e.getMessage());
                    return Response.status(Response.Status.UNAUTHORIZED)
                            .entity("Error procesando token de autenticación")
                            .build();
                }
            }

            // Buscar el usuario
            var op = usuarioRepo.findByIdOptional(id);
            if (op.isEmpty()) {
                System.out.println("Usuario no encontrado: " + id);
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Usuario no encontrado")
                        .build();
            }

            Usuario usuario = op.get();
            System.out.println("Usuario encontrado: " + usuario.getEmail());

            return Response.ok(usuario).build();

        } catch (Exception e) {
            System.err.println("Error interno al buscar usuario " + id + ": " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error interno del servidor")
                    .build();
        }
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "CLIENTE", "PROVEEDOR"})
    public Response update(@PathParam("id") Integer id, Usuario usuario,
                           @Context SecurityContext securityContext) {

        // Los usuarios solo pueden actualizar su propia información, excepto ADMIN
        if (!securityContext.isUserInRole("ADMIN")) {
            // Obtener el userId manejando el tipo JsonNumber
            Object userIdClaim = jwt.getClaim("userId");
            Integer tokenUserId = null;

            if (userIdClaim != null) {
                if (userIdClaim instanceof Number) {
                    tokenUserId = ((Number) userIdClaim).intValue();
                }
            }

            if (!tokenUserId.equals(id)) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity("No tienes permiso para ver este usuario")
                        .build();
            }
        }

        Usuario obj = usuarioRepo.findById(id);
        if (obj == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        obj.setNombre(usuario.getNombre());
        obj.setApellido(usuario.getApellido());
        obj.setTelefono(usuario.getTelefono());
        obj.setDireccion(usuario.getDireccion());
        obj.setFechaActualizacion(LocalDateTime.now());

        // Solo ADMIN puede cambiar email, rol y estado activo
        if (securityContext.isUserInRole("ADMIN")) {
            obj.setEmail(usuario.getEmail());
            obj.setRol(usuario.getRol());
            //obj.setActivo(usuario.getActivo());
        }

        return Response.ok().build();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({"ADMIN"})
    public Response delete(@PathParam("id") Integer id) {
        // Soft delete - solo marcar como inactivo
        Usuario usuario = usuarioRepo.findById(id);
        if (usuario == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        //usuario.setActivo(false);
        usuario.setFechaActualizacion(LocalDateTime.now());
        usuarioRepo.persist(usuario);

        return Response.ok().build();
    }

    /**
     * NUEVO: Subir/Actualizar imagen de perfil
     */
    @PUT
    @Path("/{id}/imagen-perfil")
    @RolesAllowed({"ADMIN", "CLIENTE", "PROVEEDOR"})
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateImagenPerfil(@PathParam("id") Integer id,
                                       Map<String, String> body,
                                       @Context SecurityContext securityContext) {
        try {
            System.out.println("Actualizando imagen de perfil usuario ID: " + id);

            Usuario usuario = usuarioRepo.findById(id);
            if (usuario == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Usuario no encontrado"))
                        .build();
            }

            // Verificar permisos
            if (!securityContext.isUserInRole("ADMIN")) {
                Integer userId = getUserIdFromJWT();
                if (!userId.equals(id)) {
                    return Response.status(Response.Status.FORBIDDEN)
                            .entity(Map.of("error", "No tienes permiso"))
                            .build();
                }
            }

            String imagenBase64 = body.get("imagenPerfil");
            if (imagenBase64 == null || imagenBase64.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "La imagen es requerida"))
                        .build();
            }

            // Validar imagen
            storageService.validateImage(imagenBase64);

            String oldImagenUrl = usuario.getImagenPerfil();

            // SUBIR NUEVA IMAGEN A S3
            String newImagenUrl = storageService.uploadImageFromBase64(
                    imagenBase64,
                    "perfiles/" + id,
                    "perfil_" + id + ".jpg"
            );

            usuario.setImagenPerfil(newImagenUrl); // GUARDAR URL DE S3
            usuario.setFechaActualizacion(LocalDateTime.now());
            usuarioRepo.persist(usuario);

            // Eliminar imagen anterior de S3
            if (oldImagenUrl != null && !oldImagenUrl.isEmpty()) {
                storageService.deleteImageByUrl(oldImagenUrl);
            }


            System.out.println("Imagen de perfil actualizada: " + newImagenUrl);

            return Response.ok(Map.of(
                    "message", "Imagen de perfil actualizada exitosamente",
                    "imagenUrl", newImagenUrl
            )).build();

        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        } catch (Exception e) {
            System.err.println("Error al actualizar imagen de perfil: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Error al actualizar imagen: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * NUEVO: Eliminar imagen de perfil
     */
    @DELETE
    @Path("/{id}/imagen-perfil")
    @RolesAllowed({"ADMIN", "CLIENTE", "PROVEEDOR"})
    public Response deleteImagenPerfil(@PathParam("id") Integer id,
                                       @Context SecurityContext securityContext) {
        try {
            Usuario usuario = usuarioRepo.findById(id);
            if (usuario == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Usuario no encontrado"))
                        .build();
            }

            // Verificar permisos
            if (!securityContext.isUserInRole("ADMIN")) {
                Integer userId = getUserIdFromJWT();
                if (!userId.equals(id)) {
                    return Response.status(Response.Status.FORBIDDEN)
                            .entity(Map.of("error", "No tienes permiso"))
                            .build();
                }
            }

            // Eliminar de S3
            if (usuario.getImagenPerfil() != null && !usuario.getImagenPerfil().isEmpty()) {
                storageService.deleteImageByUrl(usuario.getImagenPerfil());

                System.out.println("Imagen de perfil eliminada de S3");
            }

            usuario.setImagenPerfil(null);
            usuario.setFechaActualizacion(LocalDateTime.now());
            usuarioRepo.persist(usuario);

            return Response.ok(Map.of("message", "Imagen de perfil eliminada exitosamente")).build();

        } catch (Exception e) {
            System.err.println("Error al eliminar imagen de perfil: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    /**
     * Permite a un CLIENTE convertirse en PROVEEDOR (operador turístico).
     * Solo el propio usuario puede solicitar el cambio de rol.
     */
    @PUT
    @Path("/convertir-proveedor")
    @RolesAllowed({"CLIENTE"})
    public Response convertirAProveedor(@Context SecurityContext securityContext) {
        try {
            Integer userId = getUserIdFromJWT();
            System.out.println("Usuario " + userId + " solicita convertirse en PROVEEDOR");

            Usuario usuario = usuarioRepo.findById(userId);
            if (usuario == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Usuario no encontrado"))
                        .build();
            }

            // Verificar que actualmente es CLIENTE
            if ("PROVEEDOR".equalsIgnoreCase(usuario.getRol())) {
                return Response.status(Response.Status.CONFLICT)
                        .entity(Map.of(
                                "error", "Ya eres operador turístico",
                                "rol", usuario.getRol()
                        ))
                        .build();
            }

            // Cambiar rol a PROVEEDOR
            usuario.setRol("PROVEEDOR");
            usuario.setFechaActualizacion(LocalDateTime.now());
            usuarioRepo.persist(usuario);

            System.out.println("Usuario " + userId + " ahora es PROVEEDOR");

            return Response.ok(Map.of(
                    "message", "¡Felicidades! Ahora eres operador turístico.",
                    "rol", "PROVEEDOR",
                    "userId", userId
            )).build();

        } catch (Exception e) {
            System.err.println("Error al convertir a proveedor: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Error al actualizar rol: " + e.getMessage()))
                    .build();
        }
    }

    // Agregar este método auxiliar si no existe
    private Integer getUserIdFromJWT() {
        try {
            Object userIdClaim = jwt.getClaim("userId");
            if (userIdClaim instanceof Number) {
                return ((Number) userIdClaim).intValue();
            } else if (userIdClaim instanceof String) {
                return Integer.valueOf((String) userIdClaim);
            } else {
                return Integer.valueOf(userIdClaim.toString());
            }
        } catch (Exception e) {
            throw new RuntimeException("Token JWT inválido");
        }
    }
}
