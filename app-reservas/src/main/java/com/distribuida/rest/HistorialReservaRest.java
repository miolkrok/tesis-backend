package com.distribuida.rest;

import com.distribuida.db.HistorialReserva;
import com.distribuida.repo.HistorialReservaRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/historial")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transactional
public class HistorialReservaRest {

    @Inject
    private HistorialReservaRepository historialReservaRepo;

    @GET
    @Path("/{id}")
    public Response findById(@PathParam("id") Integer id) {
        System.out.println("findById");
        var op = historialReservaRepo.findByIdOptional(id);
        if (op.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(op.get()).build();
    }

    @POST
    public Response create(HistorialReserva historialReserva) {
        historialReserva.setId(null);
        historialReservaRepo.persist(historialReserva);
        return Response.status(Response.Status.CREATED).build();
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") Integer id, HistorialReserva historialReserva) {
        HistorialReserva obj = historialReservaRepo.findById(id);

        obj.setEstadoAnterior(historialReserva.getEstadoAnterior());
        obj.setEstadoNuevo(historialReserva.getEstadoNuevo());
        obj.setFechaCambio(historialReserva.getFechaCambio());
        return Response.ok().build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Integer id) {
        historialReservaRepo.deleteById(id);
        return Response.ok().build();
    }
}
