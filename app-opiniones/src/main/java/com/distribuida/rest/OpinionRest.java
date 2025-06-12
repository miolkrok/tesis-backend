package com.distribuida.rest;

import com.distribuida.clients.ActividadRestClient;
import com.distribuida.clients.UsuarioRestClient;
import com.distribuida.db.Opinion;
import com.distribuida.dtos.OpinionDTO;
import com.distribuida.repo.OpinionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/opiniones")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transactional
public class OpinionRest {

    @Inject
    private OpinionRepository opinionRepo;

    @Inject
    @RestClient
    UsuarioRestClient usuarioRestClient;

    @Inject
    @RestClient
    ActividadRestClient actividadRestClient;

    @GET
    public List<OpinionDTO> findAll() {
        var opiniones = opinionRepo.listAll();

        return opiniones.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @GET
    @Path("/{id}")
    public Response findById(@PathParam("id") Integer id) {
        System.out.println("findById opinion: " + id);
        var op = opinionRepo.findByIdOptional(id);
        if (op.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        Opinion opinion = op.get();
        try {
            OpinionDTO dto = convertToDTO(opinion);
            return Response.ok(dto).build();
        } catch (Exception e) {
            System.err.println("Error al obtener información relacionada: " + e.getMessage());
            return Response.ok(convertToDTOBasic(opinion)).build();
        }
    }

    @POST
    public Response create(Opinion opinion) {
        try {
            // Validar que exista el usuario y la actividad
            usuarioRestClient.findById(Integer.valueOf(opinion.getUsuarioId()));
            actividadRestClient.findById(Integer.valueOf(opinion.getActividadId()));

            // Validar la calificación
            if (opinion.getCalificacion() < 1 || opinion.getCalificacion() > 5) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Error: La calificación debe estar entre 1 y 5").build();
            }

            // Establecer fechas automáticamente
            if (opinion.getFechaCreacion() == null) {
                opinion.setFechaCreacion(LocalDateTime.now());
            }
            if (opinion.getFechaActualizacion() == null) {
                opinion.setFechaActualizacion(LocalDateTime.now());
            }

            opinion.setId(null);
            opinionRepo.persist(opinion);

            System.out.println("Opinión creada exitosamente con ID: " + opinion.getId());
            return Response.status(Response.Status.CREATED).entity(convertToDTO(opinion)).build();

        } catch (Exception e) {
            System.err.println("Error al crear opinión: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Error: Usuario o Actividad no encontrados").build();
        }
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") Integer id, Opinion opinion) {
        try {
            Opinion obj = opinionRepo.findById(id);
            if (obj == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            // Solo se permite actualizar calificación y comentario
            if (opinion.getCalificacion() != null) {
                if (opinion.getCalificacion() < 1 || opinion.getCalificacion() > 5) {
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity("Error: La calificación debe estar entre 1 y 5").build();
                }
                obj.setCalificacion(opinion.getCalificacion());
            }

            if (opinion.getComentario() != null) {
                obj.setComentario(opinion.getComentario());
            }

            obj.setFechaActualizacion(LocalDateTime.now());

            return Response.ok(convertToDTO(obj)).build();
        } catch (Exception e) {
            System.err.println("Error al actualizar opinión: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Integer id) {
        try {
            boolean deleted = opinionRepo.deleteById(id);
            if (!deleted) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.ok().build();
        } catch (Exception e) {
            System.err.println("Error al eliminar opinión: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Path("/usuario/{usuarioId}")
    public List<OpinionDTO> findByUsuario(@PathParam("usuarioId") String usuarioId) {
        var opiniones = opinionRepo.find("usuarioId", usuarioId).list();
        return opiniones.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @GET
    @Path("/actividad/{actividadId}")
    public List<OpinionDTO> findByActividad(@PathParam("actividadId") String actividadId) {
        var opiniones = opinionRepo.find("actividadId", actividadId).list();
        return opiniones.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @GET
    @Path("/promedio/actividad/{actividadId}")
    public Response getPromedioPuntuacion(@PathParam("actividadId") Integer actividadId) {
        var opiniones = opinionRepo.find("actividadId", actividadId.toString()).list();

        if (opiniones.isEmpty()) {
            return Response.ok(Map.of(
                    "actividadId", actividadId,
                    "promedioPuntuacion", 0.0,
                    "totalOpiniones", 0
            )).build();
        }

        double promedio = opiniones.stream()
                .mapToInt(Opinion::getCalificacion)
                .average()
                .orElse(0.0);

        return Response.ok(Map.of(
                "actividadId", actividadId,
                "promedioPuntuacion", Math.round(promedio * 100.0) / 100.0, // Redondear a 2 decimales
                "totalOpiniones", opiniones.size()
        )).build();
    }

    // Métodos auxiliares de conversión
    private OpinionDTO convertToDTO(Opinion opinion) {
        OpinionDTO dto = new OpinionDTO();
        dto.setId(opinion.getId());
        dto.setUsuarioId(opinion.getUsuarioId());
        dto.setActividadId(opinion.getActividadId());
        dto.setCalificacion(opinion.getCalificacion());
        dto.setComentario(opinion.getComentario());
        dto.setFechaCreacion(opinion.getFechaCreacion());
        dto.setFechaActualizacion(opinion.getFechaActualizacion());

        // Convertir reportes si existen
        if (opinion.getReporteOpinion() != null) {
            // Agregar conversión de reportes cuando sea necesario
        }

        return dto;
    }

    private OpinionDTO convertToDTOBasic(Opinion opinion) {
        OpinionDTO dto = new OpinionDTO();
        dto.setId(opinion.getId());
        dto.setUsuarioId(opinion.getUsuarioId());
        dto.setActividadId(opinion.getActividadId());
        dto.setCalificacion(opinion.getCalificacion());
        dto.setComentario(opinion.getComentario());
        dto.setFechaCreacion(opinion.getFechaCreacion());
        dto.setFechaActualizacion(opinion.getFechaActualizacion());
        return dto;
    }
}
