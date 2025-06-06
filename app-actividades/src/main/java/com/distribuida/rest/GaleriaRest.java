package com.distribuida.rest;

import com.distribuida.db.Actividad;
import com.distribuida.db.Galeria;
import com.distribuida.repo.ActividadRepository;
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

    @Inject
    private ActividadRepository actividadRepo;

    @GET
    public List<Galeria> findAll() {
        System.out.println("findAll imagenes");
        return galeriaRepo.listAll();
    }

    @GET
    @Path("/{id}")
    public Response findById(@PathParam("id") Integer id) {
        System.out.println("findById imagen: " + id);
        var op = galeriaRepo.findByIdOptional(id);
        if (op.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(op.get()).build();
    }

    @GET
    @Path("/actividad/{actividadId}")
    public Response findByActividadId(@PathParam("actividadId") Integer actividadId) {
        System.out.println("findByActividadId: " + actividadId);
        List<Galeria> imagenes = galeriaRepo.list("actividad.id", actividadId);
        return Response.ok(imagenes).build();
    }

    @POST
    public Response create(Galeria galeria) {
        try {
            galeria.setId(null);

            // Validar que la actividad existe
            if (galeria.getActividad() != null && galeria.getActividad().getId() != null) {
                Actividad actividad = actividadRepo.findById(galeria.getActividad().getId());
                if (actividad == null) {
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity("La actividad especificada no existe").build();
                }
                galeria.setActividad(actividad);
            }

            galeriaRepo.persist(galeria);
            System.out.println("Imagen creada exitosamente con ID: " + galeria.getId());
            return Response.status(Response.Status.CREATED).entity(galeria).build();
        } catch (Exception e) {
            System.err.println("Error al crear imagen: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al crear imagen: " + e.getMessage()).build();
        }
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") Integer id, Galeria galeria) {
        try {
            Galeria obj = galeriaRepo.findById(id);
            if (obj == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            obj.setUrlFoto(galeria.getUrlFoto());

            // Actualizar actividad si se proporciona
            if (galeria.getActividad() != null && galeria.getActividad().getId() != null) {
                Actividad actividad = actividadRepo.findById(galeria.getActividad().getId());
                if (actividad != null) {
                    obj.setActividad(actividad);
                }
            }

            return Response.ok(obj).build();
        } catch (Exception e) {
            System.err.println("Error al actualizar imagen: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Integer id) {
        try {
            boolean deleted = galeriaRepo.deleteById(id);
            if (!deleted) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.ok().build();
        } catch (Exception e) {
            System.err.println("Error al eliminar imagen: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }


}
