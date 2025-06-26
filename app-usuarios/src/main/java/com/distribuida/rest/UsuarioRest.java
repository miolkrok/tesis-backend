package com.distribuida.rest;

import com.distribuida.db.Usuario;
import com.distribuida.repo.UsuarioRepository;
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
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.time.LocalDateTime;
import java.util.List;

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
    JsonWebToken jwt;

    @GET
    @RolesAllowed({"ADMIN"})
    public List<Usuario> findAll() {
        System.out.println("findAll usuarios - Admin access");
        return usuarioRepo.listAll();
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "CLIENTE", "PROVEEDOR"})
    public Response findById(@PathParam("id") Integer id,
                             @Context SecurityContext securityContext) {

        // Los usuarios solo pueden ver su propia información, excepto ADMIN
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

        var op = usuarioRepo.findByIdOptional(id);
        if (op.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(op.get()).build();
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
}
