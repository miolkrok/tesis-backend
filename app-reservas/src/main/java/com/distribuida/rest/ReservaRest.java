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

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @GET
    @Path("/{id}")
    public Response findById(@PathParam("id") Integer id) {
        System.out.println("findById reserva: " + id);
        var op = reservaRepo.findByIdOptional(id);
        if (op.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        Reserva reserva = op.get();
        try {
            ReservaDTO dto = convertToDTO(reserva);
            return Response.ok(dto).build();
        } catch (Exception e) {
            System.err.println("Error al obtener información relacionada: " + e.getMessage());
            return Response.ok(convertToDTOBasic(reserva)).build();
        }
    }

    @POST
    public Response create(Reserva reserva) {
        try {
            // Validar que existan el usuario y la actividad
            usuarioRestClient.findById(reserva.getUsuarioId());
            actividadRestClient.findById(reserva.getActividadId());

            // Establecer fechas automáticamente
            if (reserva.getFechaCreacion() == null) {
                reserva.setFechaCreacion(LocalDateTime.now());
            }
            if (reserva.getFechaActualizacion() == null) {
                reserva.setFechaActualizacion(LocalDateTime.now());
            }
            if (reserva.getFechaReserva() == null) {
                reserva.setFechaReserva(LocalDateTime.now());
            }

            // Estado por defecto
            if (reserva.getEstado() == null) {
                reserva.setEstado("PENDIENTE");
            }

            reserva.setId(null);
            reservaRepo.persist(reserva);

            System.out.println("Reserva creada exitosamente con ID: " + reserva.getId());
            return Response.status(Response.Status.CREATED).entity(convertToDTO(reserva)).build();

        } catch (Exception e) {
            System.err.println("Error al crear reserva: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Error: Usuario o Actividad no encontrados").build();
        }
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") Integer id, Reserva reserva) {
        try {
            Reserva obj = reservaRepo.findById(id);
            if (obj == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            // Actualizar campos permitidos
            if (reserva.getEstado() != null) {
                obj.setEstado(reserva.getEstado());
            }
            if (reserva.getFechaActividad() != null) {
                obj.setFechaActividad(reserva.getFechaActividad());
            }
            if (reserva.getCantidadPersonas() != null) {
                obj.setCantidadPersonas(reserva.getCantidadPersonas());
            }
            if (reserva.getCostoTotal() != null) {
                obj.setCostoTotal(reserva.getCostoTotal());
            }

            obj.setFechaActualizacion(LocalDateTime.now());

            return Response.ok(convertToDTO(obj)).build();
        } catch (Exception e) {
            System.err.println("Error al actualizar reserva: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Integer id) {
        try {
            boolean deleted = reservaRepo.deleteById(id);
            if (!deleted) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.ok().build();
        } catch (Exception e) {
            System.err.println("Error al eliminar reserva: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Path("/usuario/{usuarioId}")
    public List<ReservaDTO> findByUsuario(@PathParam("usuarioId") Integer usuarioId) {
        var reservas = reservaRepo.find("usuarioId", usuarioId).list();
        return reservas.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @GET
    @Path("/actividad/{actividadId}")
    public List<ReservaDTO> findByActividad(@PathParam("actividadId") Integer actividadId) {
        var reservas = reservaRepo.find("actividadId", actividadId).list();
        return reservas.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @GET
    @Path("/estado/{estado}")
    public List<ReservaDTO> findByEstado(@PathParam("estado") String estado) {
        var reservas = reservaRepo.find("estado", estado).list();
        return reservas.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Endpoint para cambiar estado de reserva
    @PUT
    @Path("/{id}/estado")
    public Response cambiarEstado(@PathParam("id") Integer id, @QueryParam("nuevoEstado") String nuevoEstado) {
        try {
            Reserva reserva = reservaRepo.findById(id);
            if (reserva == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            String estadoAnterior = reserva.getEstado();
            reserva.setEstado(nuevoEstado);
            reserva.setFechaActualizacion(LocalDateTime.now());

            System.out.println("Estado de reserva " + id + " cambiado de " + estadoAnterior + " a " + nuevoEstado);

            return Response.ok(convertToDTO(reserva)).build();
        } catch (Exception e) {
            System.err.println("Error al cambiar estado de reserva: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Métodos de conversión
    private ReservaDTO convertToDTO(Reserva reserva) {
        ReservaDTO dto = new ReservaDTO();
        dto.setId(reserva.getId());
        dto.setActividadId(reserva.getActividadId());
        dto.setUsuarioId(reserva.getUsuarioId());
        dto.setEstado(reserva.getEstado());
        dto.setFechaReserva(reserva.getFechaReserva());
        dto.setFechaActividad(reserva.getFechaActividad());
        dto.setCantidadPersonas(reserva.getCantidadPersonas());
        dto.setCostoTotal(reserva.getCostoTotal());
        dto.setFechaCreacion(reserva.getFechaCreacion());
        dto.setFechaActualizacion(reserva.getFechaActualizacion());

        // Incluir historial si existe
        if (reserva.getHistorialReserva() != null) {
            dto.setHistorialReserva(reserva.getHistorialReserva());
        }

        return dto;
    }

    private ReservaDTO convertToDTOBasic(Reserva reserva) {
        ReservaDTO dto = new ReservaDTO();
        dto.setId(reserva.getId());
        dto.setActividadId(reserva.getActividadId());
        dto.setUsuarioId(reserva.getUsuarioId());
        dto.setEstado(reserva.getEstado());
        dto.setFechaReserva(reserva.getFechaReserva());
        dto.setFechaActividad(reserva.getFechaActividad());
        dto.setCantidadPersonas(reserva.getCantidadPersonas());
        dto.setCostoTotal(reserva.getCostoTotal());
        dto.setFechaCreacion(reserva.getFechaCreacion());
        dto.setFechaActualizacion(reserva.getFechaActualizacion());
        return dto;
    }
}
