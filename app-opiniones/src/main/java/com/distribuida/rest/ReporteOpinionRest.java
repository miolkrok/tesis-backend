package com.distribuida.rest;

import com.distribuida.db.ReporteOpinion;
import com.distribuida.repo.ReporteOpinionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/reportesOpinion")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transactional
public class ReporteOpinionRest {

    @Inject
    private ReporteOpinionRepository reporteOpinionRepo;

    @GET
    public List<ReporteOpinion> findAll() {
        System.out.println("findAll");
        return reporteOpinionRepo.listAll();
    }

    @GET
    @Path("/{id}")
    public Response findById(@PathParam("id") Integer id) {
        System.out.println("findById");
        var op = reporteOpinionRepo.findByIdOptional(id);
        if (op.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(op.get()).build();
    }

    @POST
    public Response create(ReporteOpinion reporteOpinion) {
        reporteOpinion.setId(null);
        reporteOpinionRepo.persist(reporteOpinion);
        return Response.status(Response.Status.CREATED).build();
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") Integer id, ReporteOpinion reporteOpinion) {
        ReporteOpinion obj = reporteOpinionRepo.findById(id);
        obj.setRazon(reporteOpinion.getRazon());
        obj.setEstado(reporteOpinion.getEstado());
        obj.setFechaReporte(reporteOpinion.getFechaReporte());
        return Response.ok().build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Integer id) {
        reporteOpinionRepo.deleteById(id);
        return Response.ok().build();
    }

}
