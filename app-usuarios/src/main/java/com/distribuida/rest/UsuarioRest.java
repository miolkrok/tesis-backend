package com.distribuida.rest;

import com.distribuida.db.Usuario;
import com.distribuida.repo.UsuarioRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.List;

@Path("/usuarios")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transactional
public class UsuarioRest {

    @Inject
    private UsuarioRepository usuarioRepo;

    @GET
    public List<Usuario> findAll() {
        System.out.println("findAll");
        return usuarioRepo.listAll();
    }

    @GET
    @Path("/{id}")
    public Response findById(@PathParam("id") Integer id) {
        System.out.println("findById");
        var op = usuarioRepo.findByIdOptional(id);
        if (op.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(op.get()).build();
    }

    @POST
    public Response create(Usuario usuario) {
        usuario.setId(null);
        usuarioRepo.persist(usuario);
        return Response.status(Response.Status.CREATED).build();
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") Integer id, Usuario usuario) {
        Usuario obj = usuarioRepo.findById(id);

        obj.setNombre(usuario.getNombre());
        obj.setApellido(usuario.getApellido());
        obj.setEmail(usuario.getEmail());
        obj.setTelefono(usuario.getTelefono());
        obj.setPassword(usuario.getPassword());
        obj.setEmail(usuario.getEmail());
        obj.setDireccion(usuario.getDireccion());
        obj.setFechaCreacion(usuario.getFechaCreacion());
        obj.setFechaActualizacion(usuario.getFechaActualizacion());
        return Response.ok().build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Integer id) {
        usuarioRepo.deleteById(id);
        return Response.ok().build();
    }
}
