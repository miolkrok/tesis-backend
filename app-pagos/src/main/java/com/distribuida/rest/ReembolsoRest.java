package com.distribuida.rest;

import com.distribuida.db.Reembolso;
import com.distribuida.repo.ReembolsoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/reembolsos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transactional
public class ReembolsoRest {

    @Inject
    private ReembolsoRepository reembolsoRepo;

    @GET
    public List<Reembolso> findAll() {
        System.out.println("findAll");
        return reembolsoRepo.listAll();
    }

    @GET
    @Path("/{id}")
    public Response findById(@PathParam("id") Integer id) {
        System.out.println("findById");
        var op = reembolsoRepo.findByIdOptional(id);
        if (op.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(op.get()).build();
    }

    @POST
    public Response create(Reembolso reembolso) {
        reembolso.setId(null);
        reembolsoRepo.persist(reembolso);
        return Response.status(Response.Status.CREATED).build();
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") Integer id, Reembolso reembolso) {
        Reembolso obj = reembolsoRepo.findById(id);

        obj.setMonto(reembolso.getMonto());
        obj.setFechaReembolso(reembolso.getFechaReembolso());
        obj.setEstado(reembolso.getEstado());
        return Response.ok().build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Integer id) {
        reembolsoRepo.deleteById(id);
        return Response.ok().build();
    }
}
