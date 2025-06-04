package com.distribuida.rest;

import com.distribuida.db.Actividad;
import com.distribuida.db.Galeria;
import com.distribuida.db.ServicioEvento;
import com.distribuida.repo.ActividadRepository;
import com.distribuida.repo.ServicioEventoRepository;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

import java.util.List;

public class ServicioEventoRest {

    @Inject
    private ServicioEventoRepository servicioEventoRepo;

    @Inject
    private ActividadRepository actividadRepo;

    @GET
    public List<ServicioEvento> findAll() {
        System.out.println("findAll servicios evento");
        return servicioEventoRepo.listAll();
    }

    @GET
    @Path("/{id}")
    public Response findById(@PathParam("id") Integer id) {
        System.out.println("findById servicio evento: " + id);
        var op = servicioEventoRepo.findByIdOptional(id);
        if (op.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(op.get()).build();
    }

    @GET
    @Path("/actividad/{actividadId}")
    public Response findByActividadId(@PathParam("actividadId") Integer actividadId) {
        System.out.println("findByActividadId servicios: " + actividadId);
        List<ServicioEvento> servicios = servicioEventoRepo.list("actividadServicio.id", actividadId);
        return Response.ok(servicios).build();
    }

    @POST
    public Response create(ServicioEvento servicioEvento) {
        try {
            servicioEvento.setId(null);

            // Validar que la actividad existe
            if (servicioEvento.getActividadServicio() != null && servicioEvento.getActividadServicio().getId() != null) {
                Actividad actividad = actividadRepo.findById(servicioEvento.getActividadServicio().getId());
                if (actividad == null) {
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity("La actividad especificada no existe").build();
                }
                servicioEvento.setActividadServicio(actividad);
            }

            servicioEventoRepo.persist(servicioEvento);
            System.out.println("Servicio evento creado exitosamente con ID: " + servicioEvento.getId());
            return Response.status(Response.Status.CREATED).entity(servicioEvento).build();
        } catch (Exception e) {
            System.err.println("Error al crear servicio evento: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al crear servicio evento: " + e.getMessage()).build();
        }
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") Integer id, ServicioEvento servicioEvento) {
        try {
            ServicioEvento obj = servicioEventoRepo.findById(id);
            if (obj == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            obj.setListaServicio(servicioEvento.getListaServicio());

            // Actualizar actividad si se proporciona
            if (servicioEvento.getActividadServicio() != null && servicioEvento.getActividadServicio().getId() != null) {
                Actividad actividad = actividadRepo.findById(servicioEvento.getActividadServicio().getId());
                if (actividad != null) {
                    obj.setActividadServicio(actividad);
                }
            }

            return Response.ok(obj).build();
        } catch (Exception e) {
            System.err.println("Error al actualizar servicio evento: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Integer id) {
        try {
            boolean deleted = servicioEventoRepo.deleteById(id);
            if (!deleted) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.ok().build();
        } catch (Exception e) {
            System.err.println("Error al eliminar servicio evento: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
