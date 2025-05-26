package com.distribuida.rest;

import com.distribuida.clients.ActividadRestClient;
import com.distribuida.clients.UsuarioRestClient;
import com.distribuida.db.Reserva;
import com.distribuida.dtos.ReservaDTO;
import com.distribuida.repo.ReservaRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.List;

@Path("/reservas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transactional
public class ReservaRest {

    @Inject
    ReservaRepository reservaRepo;

    @Inject
    @RestClient
    UsuarioRestClient usuarioRestClient;

    @Inject
    @RestClient
    ActividadRestClient actividadRestClient;

    @GET
    public List<ReservaDTO> findAll() {
        var reservas = reservaRepo.listAll();

        return reservas.stream()
                .map(reserva -> {
                    try {
                        var usuario = usuarioRestClient.findById(reserva.getUsuarioId());
                        var actividad = actividadRestClient.findById(reserva.getActividadId());

                        ReservaDTO dto = new ReservaDTO();
                        dto.setId(reserva.getId());
                        //arreglar
                        //dto.setFechaInicio(reserva.getFechaInicio());
                        //dto.setFechaFin(reserva.getFechaFin());
                        dto.setEstado(reserva.getEstado());
                        //dto.setPrecioTotal(reserva.getPrecioTotal());
                        //dto.setNumeroPersonas(reserva.getNumeroPersonas());

                        // Información enriquecida desde otros servicios
                        //dto.setNombreUsuario(usuario.getNombre() + " " + usuario.getApellido());
                        //dto.setNombreActividad(actividad.getTitulo());

                        return dto;
                    } catch (Exception e) {
                        // Si hay un error al obtener información de otros servicios
                        ReservaDTO dto = new ReservaDTO();
                        dto.setId(reserva.getId());
                        //dto.setFechaInicio(reserva.getFechaInicio());
                        //dto.setFechaFin(reserva.getFechaFin());
                        dto.setEstado(reserva.getEstado());
                        //dto.setPrecioTotal(reserva.getPrecioTotal());
                        //dto.setNumeroPersonas(reserva.getNumeroPersonas());

                        return dto;
                    }
                }).toList();
    }

    @GET
    @Path("/{id}")
    public Response findById(@PathParam("id") Integer id) {
        System.out.println("findById");
        var op = reservaRepo.findByIdOptional(id);
        if (op.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        Reserva reserva = op.get();
        try {
            var usuario = usuarioRestClient.findById(reserva.getUsuarioId());
            var actividad = actividadRestClient.findById(reserva.getActividadId());

            ReservaDTO dto = new ReservaDTO();
            dto.setId(reserva.getId());
            //dto.setFechaInicio(reserva.getFechaInicio());
            //dto.setFechaFin(reserva.getFechaFin());
            dto.setEstado(reserva.getEstado());
            //dto.setPrecioTotal(reserva.getPrecioTotal());
            //dto.setNumeroPersonas(reserva.getNumeroPersonas());
            //dto.setNombreUsuario(usuario.getNombre() + " " + usuario.getApellido());
            //dto.setNombreActividad(actividad.getTitulo());

            return Response.ok(dto).build();
        } catch (Exception e) {
            // Si no se puede obtener información de otros servicios
            return Response.ok(reserva).build();
        }
    }

    @POST
    public Response create(Reserva reserva) {
        try {
            usuarioRestClient.findById(reserva.getUsuarioId());
            actividadRestClient.findById(reserva.getActividadId());

            reserva.setId(null);
            reservaRepo.persist(reserva);
            return Response.status(Response.Status.CREATED).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Error: Usuario o Actividad no encontrados").build();
        }
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") Integer id, Reserva reserva) {
        Reserva obj = reservaRepo.findById(id);

        obj.setEstado(reserva.getEstado());
        obj.setFechaReserva(reserva.getFechaReserva());
        obj.setFechaActividad(reserva.getFechaActividad());
        obj.setCantidadPersonas(reserva.getCantidadPersonas());
        obj.setCostoTotal(reserva.getCostoTotal());
        obj.setFechaCreacion(reserva.getFechaCreacion());
        obj.setFechaActualizacion(reserva.getFechaActualizacion());
        return Response.ok().build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Integer id) {
        reservaRepo.deleteById(id);
        return Response.ok().build();
    }

    @GET
    @Path("/usuario/{usuarioId}")
    public List<ReservaDTO> findByUsuario(@PathParam("usuarioId") Integer usuarioId) {
        var reservas = reservaRepo.find("usuarioId", usuarioId).list();

        return reservas.stream()
                .map(reserva -> {
                    try {
                        var usuario = usuarioRestClient.findById(reserva.getUsuarioId());
                        var actividad = actividadRestClient.findById(reserva.getActividadId());

                        ReservaDTO dto = new ReservaDTO();
                        dto.setId(reserva.getId());
                        //dto.setFechaInicio(reserva.getFechaInicio());
                        //dto.setFechaFin(reserva.getFechaFin());
                        dto.setEstado(reserva.getEstado());
                        //dto.setPrecioTotal(reserva.getPrecioTotal());
                        //dto.setNumeroPersonas(reserva.getNumeroPersonas());
                        //dto.setNombreUsuario(usuario.getNombre() + " " + usuario.getApellido());
                        //dto.setNombreActividad(actividad.getTitulo());

                        return dto;
                    } catch (Exception e) {
                        ReservaDTO dto = new ReservaDTO();
                        dto.setId(reserva.getId());
                        //dto.setFechaInicio(reserva.getFechaInicio());
                        //dto.setFechaFin(reserva.getFechaFin());
                        dto.setEstado(reserva.getEstado());
                        //dto.setPrecioTotal(reserva.getPrecioTotal());
                        //dto.setNumeroPersonas(reserva.getNumeroPersonas());

                        return dto;
                    }
                }).toList();
    }

    @GET
    @Path("/actividad/{actividadId}")
    public List<ReservaDTO> findByActividad(@PathParam("actividadId") Integer actividadId) {
        var reservas = reservaRepo.find("actividadId", actividadId).list();

        return reservas.stream()
                .map(reserva -> {
                    try {
                        var usuario = usuarioRestClient.findById(reserva.getUsuarioId());
                        var actividad = actividadRestClient.findById(reserva.getActividadId());

                        ReservaDTO dto = new ReservaDTO();
                        dto.setId(reserva.getId());
                        //dto.setFechaInicio(reserva.getFechaInicio());
                        //dto.setFechaFin(reserva.getFechaFin());
                        dto.setEstado(reserva.getEstado());
                        //dto.setPrecioTotal(reserva.getPrecioTotal());
                        //dto.setNumeroPersonas(reserva.getNumeroPersonas());
                        //dto.setNombreUsuario(usuario.getNombre() + " " + usuario.getApellido());
                        //dto.setNombreActividad(actividad.getTitulo());

                        return dto;
                    } catch (Exception e) {
                        ReservaDTO dto = new ReservaDTO();
                        dto.setId(reserva.getId());
                        //dto.setFechaInicio(reserva.getFechaInicio());
                        //dto.setFechaFin(reserva.getFechaFin());
                        dto.setEstado(reserva.getEstado());
                        //dto.setPrecioTotal(reserva.getPrecioTotal());
                        //dto.setNumeroPersonas(reserva.getNumeroPersonas());

                        return dto;
                    }
                }).toList();
    }
}
