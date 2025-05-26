package com.distribuida.rest;

import com.distribuida.db.Actividad;
import com.distribuida.db.Galeria;
import com.distribuida.repo.GaleriaRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/imagenes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transactional
public class GaleriaRest {

    @Inject
    private GaleriaRepository galeriaRepo;

    @GET
    public List<Galeria> findAll() {
        System.out.println("findAll");
        return galeriaRepo.listAll();
    }

    @GET
    @Path("/{id}")
    public Response findById(@PathParam("id") Integer id) {
        System.out.println("findById");
        var op = galeriaRepo.findByIdOptional(id);
        if (op.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(op.get()).build();
    }

    @POST
    public Response create(Galeria galeria) {
        galeria.setId(null);
        galeriaRepo.persist(galeria);
        return Response.status(Response.Status.CREATED).build();
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") Integer id, Galeria galeria) {
        Galeria obj = galeriaRepo.findById(id);

        obj.setUrlFoto(galeria.getUrlFoto());
        return Response.ok().build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Integer id) {
        galeriaRepo.deleteById(id);
        return Response.ok().build();
    }


}
