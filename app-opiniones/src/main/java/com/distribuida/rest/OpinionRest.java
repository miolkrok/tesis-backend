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

@Path("/opiniones")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transactional
public class OpinionRest {

    @Inject
    private OpinionRepository opinonRepo;

    @Inject
    @RestClient
    UsuarioRestClient usuarioRestClient;

    @Inject
    @RestClient
    ActividadRestClient actividadRestClient;

    @GET
    public List<OpinionDTO> findAll() {
        var opiniones = opinonRepo.listAll();

        return opiniones.stream()
                .map(opinion -> {
                    try {
                        var usuario = usuarioRestClient.findById(resena.getUsuarioId());
                        var actividad = actividadRestClient.findById(resena.getActividadId());

                        OpinionDTO dto = new OpinionDTO();
                        dto.setId(opinion.getId());
                        dto.setUsuarioId(opinion.getUsuarioId());
                        dto.setActividadId(opinion.getActividadId());
                        //dto.setPuntuacion(opinion.getPuntuacion());
                        dto.setComentario(opinion.getComentario());
                        dto.setFechaCreacion(opinion.getFechaCreacion());

                        // Información enriquecida desde otros servicios
                        //dto.setNombreUsuario(usuario.getNombre() + " " + usuario.getApellido());
                        //dto.setTituloActividad(actividad.getTitulo());

                        return dto;
                    } catch (Exception e) {
                        OpinionDTO dto = new OpinionDTO();
                        dto.setId(opinion.getId());
                        dto.setUsuarioId(opinion.getUsuarioId());
                        dto.setActividadId(opinion.getActividadId());
                        //dto.setPuntuacion(opinion.getPuntuacion());
                        dto.setComentario(opinion.getComentario());
                        dto.setFechaCreacion(opinion.getFechaCreacion());

                        return dto;
                    }
                }).toList();
    }

    @GET
    @Path("/{id}")
    public Response findById(@PathParam("id") Integer id) {
        System.out.println("findById");
        var op = opinonRepo.findByIdOptional(id);
        if (op.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        Opinion opinion = op.get();
        try {
            var usuario = usuarioRestClient.findById(opinion.getUsuarioId());
            var actividad = actividadRestClient.findById(opinion.getActividadId());

            OpinionDTO dto = new OpinionDTO();
            dto.setId(opinion.getId());
            dto.setUsuarioId(opinion.getUsuarioId());
            dto.setActividadId(opinion.getActividadId());
            //dto.setPuntuacion(opinion.getPuntuacion());
            dto.setComentario(opinion.getComentario());
            dto.setFechaCreacion(opinion.getFechaCreacion());
            //dto.setNombreUsuario(usuario.getNombre() + " " + usuario.getApellido());
            //dto.setTituloActividad(actividad.getTitulo());

            return Response.ok(dto).build();
        } catch (Exception e) {
            return Response.ok(opinion).build();
        }
    }

    @POST
    public Response create(Opinion opinion) {
        // Validar que exista el usuario y la actividad
        try {
            usuarioRestClient.findById(opinion.getUsuarioId());
            actividadRestClient.findById(opinion.getActividadId());

            // Validar la puntuación
            /*if (opinion.getPuntuacion() < 1 || opinion.getPuntuacion() > 5) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Error: La puntuación debe estar entre 1 y 5").build();
            }*/

            // Establecer fecha actual si no se proporcionó
            if (opinion.getFechaCreacion() == null) {
                opinion.setFechaCreacion(LocalDateTime.now());
            }

            opinion.setId(null);
            opinonRepo.persist(opinion);
            return Response.status(Response.Status.CREATED).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Error: Usuario o Actividad no encontrados").build();
        }
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") Integer id, Opinion opinion) {
        Opinion obj = opinonRepo.findById(id);

        // Solo se permite actualizar puntuación y comentario
        //obj.setPuntuacion(opinion.getPuntuacion());
        obj.setComentario(opinion.getComentario());

        return Response.ok().build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Integer id) {
        opinonRepo.deleteById(id);
        return Response.ok().build();
    }

    @GET
    @Path("/usuario/{usuarioId}")
    public List<OpinionDTO> findByUsuario(@PathParam("usuarioId") Integer usuarioId) {
        var resenas = opinonRepo.find("usuarioId", usuarioId).list();

        return resenas.stream()
                .map(resena -> {
                    try {
                        var usuario = usuarioRestClient.findById(resena.getUsuarioId());
                        var actividad = actividadRestClient.findById(resena.getActividadId());

                        OpinionDTO dto = new OpinionDTO();
                        dto.setId(resena.getId());
                        dto.setUsuarioId(resena.getUsuarioId());
                        dto.setActividadId(resena.getActividadId());
                        //dto.setPuntuacion(resena.getPuntuacion());
                        dto.setComentario(resena.getComentario());
                        dto.setFechaCreacion(resena.getFechaCreacion());
                        //dto.setNombreUsuario(usuario.getNombre() + " " + usuario.getApellido());
                        //dto.setTituloActividad(actividad.getTitulo());

                        return dto;
                    } catch (Exception e) {
                        OpinionDTO dto = new OpinionDTO();
                        dto.setId(resena.getId());
                        dto.setUsuarioId(resena.getUsuarioId());
                        dto.setActividadId(resena.getActividadId());
                        //dto.setPuntuacion(resena.getPuntuacion());
                        dto.setComentario(resena.getComentario());
                        dto.setFechaCreacion(resena.getFechaCreacion());

                        return dto;
                    }
                    }).toList();
    }

    @GET
    @Path("/actividad/{actividadId}")
    public List<OpinionDTO> findByActividad(@PathParam("actividadId") Integer actividadId) {
        var resenas = opinonRepo.find("actividadId", actividadId).list();

        return resenas.stream()
                .map(resena -> {
                    try {
                        var usuario = usuarioRestClient.findById(resena.getUsuarioId());
                        var actividad = actividadRestClient.findById(resena.getActividadId());

                        OpinionDTO dto = new OpinionDTO();
                        dto.setId(resena.getId());
                        dto.setUsuarioId(resena.getUsuarioId());
                        dto.setActividadId(resena.getActividadId());
                        //dto.setPuntuacion(resena.getPuntuacion());
                        dto.setComentario(resena.getComentario());
                        dto.setFechaCreacion(resena.getFechaCreacion());
                        //dto.setNombreUsuario(usuario.getNombre() + " " + usuario.getApellido());
                        //dto.setTituloActividad(actividad.getTitulo());

                        return dto;
                    } catch (Exception e) {
                        OpinionDTO dto = new OpinionDTO();
                        dto.setId(resena.getId());
                        dto.setUsuarioId(resena.getUsuarioId());
                        dto.setActividadId(resena.getActividadId());
                        //dto.setPuntuacion(resena.getPuntuacion());
                        dto.setComentario(resena.getComentario());
                        dto.setFechaCreacion(resena.getFechaCreacion());

                        return dto;
                    }
                }).toList();
    }

    @GET
    @Path("/promedio/actividad/{actividadId}")
    public Response getPromedioPuntuacion(@PathParam("actividadId") Integer actividadId) {
        //var opiniones = opinonRepo.find("actividadId", actividadId).list();

        /*if (opiniones.isEmpty()) {
            return Response.ok(Map.of(
                    "actividadId", actividadId,
                    "promedioPuntuacion", 0,
                    "totalResenas", 0
            )).build();
        }

        double promedio = opiniones.stream()
                .mapToInt(Opinion::getPuntuacion)
                .average()
                .orElse(0);

        return Response.ok(Map.of(
                "actividadId", actividadId,
                "promedioPuntuacion", promedio,
                "totalResenas", resenas.size()
        )).build();*/
        return null;
    }
}
