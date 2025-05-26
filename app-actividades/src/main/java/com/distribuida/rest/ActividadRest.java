package com.distribuida.rest;

import com.distribuida.clients.ReservaRestClient;
import com.distribuida.clients.UsuarioRestClient;
import com.distribuida.db.Actividad;
import com.distribuida.dtos.ActividadDTO;
import com.distribuida.repo.ActividadRepository;
import com.distribuida.repo.GaleriaRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.time.LocalDateTime;
import java.util.List;

@Path("/actividades")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transactional
public class ActividadRest {

    @Inject
    private ActividadRepository actividadRepo;

    @Inject
    @RestClient
    UsuarioRestClient usuarioRestClient;

    @GET
    public List<ActividadDTO> findAll() {
        var actividades = actividadRepo.listAll();

        return actividades.stream()
                .map(actividad -> {
                    var proveedor = usuarioRestClient.findById(actividad.getProveedorId());

                    ActividadDTO dto = new ActividadDTO();
                    dto.setId(actividad.getId());
                    dto.setTitulo(actividad.getTitulo());
                    dto.setDescripcion(actividad.getDescripcion());
                    dto.setUbicacion(actividad.getUbicacion());
                    dto.setPrecio(actividad.getPrecio());
                    dto.setDuracion(actividad.getDuracion());
                    dto.setNivelDificultad(actividad.getNivelDificultad());
                    dto.setTipoActividad(actividad.getTipoActividad());
                    dto.setDisponibilidad(actividad.getDisponibilidad());
                    dto.setFechaCreacion(LocalDateTime.now());
                    dto.setFechaActualizacion(LocalDateTime.now());
                    dto.setProveedorId(proveedor.getId());

                    return dto;
                }).toList();
    }

    @GET
    @Path("/{id}")
    public Response findById(@PathParam("id") Integer id) {
        var op = actividadRepo.findByIdOptional(id);
        if (op.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        Actividad actividad = op.get();
        try {
            var proveedor = usuarioRestClient.findById(actividad.getProveedorId());

            ActividadDTO dto = new ActividadDTO();
            dto.setId(actividad.getId());
            dto.setTitulo(actividad.getTitulo());
            dto.setDescripcion(actividad.getDescripcion());
            dto.setUbicacion(actividad.getUbicacion());
            dto.setPrecio(actividad.getPrecio());
            dto.setDuracion(actividad.getDuracion());
            dto.setNivelDificultad(actividad.getNivelDificultad());
            dto.setTipoActividad(actividad.getTipoActividad());
            dto.setDisponibilidad(actividad.getDisponibilidad());
            dto.setFechaCreacion(LocalDateTime.now());
            dto.setFechaActualizacion(LocalDateTime.now());
            dto.setProveedorId(proveedor.getId());

            return Response.ok(dto).build();
        } catch (Exception e) {
            // Si no se puede obtener la información del proveedor, devolver la actividad sin esa info
            return Response.ok(actividad).build();
        }
    }

    @POST
    public Response create(Actividad actividad) {
        actividad.setId(null);
        actividadRepo.persist(actividad);
        return Response.status(Response.Status.CREATED).build();
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") Integer id, Actividad actividad) {
        Actividad obj = actividadRepo.findById(id);

        obj.setTitulo(actividad.getTitulo());
        obj.setDescripcion(actividad.getDescripcion());
        obj.setUbicacion(actividad.getUbicacion());
        obj.setPrecio(actividad.getPrecio());
        obj.setDuracion(actividad.getDuracion());
        obj.setNivelDificultad(actividad.getNivelDificultad());
        obj.setTipoActividad(actividad.getTipoActividad());
        obj.setDisponibilidad(actividad.getDisponibilidad());
        obj.setFechaCreacion(actividad.getFechaCreacion());
        obj.setFechaActualizacion(LocalDateTime.now());
        obj.setProveedorId(actividad.getId());

        return Response.ok().build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Integer id) {
        actividadRepo.deleteById(id);
        return Response.ok().build();
    }
}

