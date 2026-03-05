package com.distribuida.rest;

import com.distribuida.clients.ActividadRestClient;
import com.distribuida.clients.UsuarioRestClient;
import com.distribuida.db.Reserva;
import com.distribuida.dtos.ReservaDTO;
import com.distribuida.repo.ReservaRepository;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/reservas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transactional
@Authenticated
public class ReservaRest {

    @Inject
    ReservaRepository reservaRepo;

    @Inject
    @RestClient
    UsuarioRestClient usuarioRestClient;

    @Inject
    @RestClient
    ActividadRestClient actividadRestClient;

    @Inject
    JsonWebToken jwt;

    @GET
    //@RolesAllowed({"ADMIN", "CLIENTE", "PROVEEDOR"})
    @PermitAll
    public List<ReservaDTO> findAll(@Context SecurityContext securityContext) {
        // Solo ADMIN puede ver todas las reservas
        if (securityContext.isUserInRole("ADMIN")) {
            var reservas = reservaRepo.listAll();
            return reservas.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } else {
            // CLIENTE y PROVEEDOR solo ven sus propias reservas
            Integer userId = getUserIdFromJWT();
            var reservas = reservaRepo.find("usuarioId", userId).list();
            return reservas.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        }
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "CLIENTE", "PROVEEDOR"})
    public Response findById(@PathParam("id") Integer id, @Context SecurityContext securityContext) {
        System.out.println("Buscando reserva ID: " + id + " por usuario: " + getUserIdFromJWT());

        var op = reservaRepo.findByIdOptional(id);
        if (op.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        Reserva reserva = op.get();

        // Verificar permisos: solo el propietario o ADMIN pueden ver la reserva
        if (!securityContext.isUserInRole("ADMIN")) {
            Integer userId = getUserIdFromJWT();
            if (!reserva.getUsuarioId().equals(userId)) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity("No tienes permiso para ver esta reserva")
                        .build();
            }
        }

        try {
            ReservaDTO dto = convertToDTO(reserva);
            return Response.ok(dto).build();
        } catch (Exception e) {
            System.err.println("Error al obtener información relacionada: " + e.getMessage());
            return Response.ok(convertToDTOBasic(reserva)).build();
        }
    }

    @POST
    @PermitAll
    public Response create(@Valid ReservaDTO reservaDTO) {
        try {
            Integer userId = getUserIdFromJWT();
            System.out.println("Creando reserva para usuario: " + userId + " | Actividad: " + reservaDTO.getActividadId());

            // Validaciones básicas
            if (reservaDTO.getActividadId() == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "El ID de la actividad es requerido")).build();
            }

            // Asignar automáticamente el usuario del JWT
            reservaDTO.setUsuarioId(userId);

            // VALIDACIÓN DE CUPO POR FECHA
            if (reservaDTO.getFechaActividad() != null && reservaDTO.getCantidadPersonas() != null) {
                // Obtener todas las reservas para esta actividad en la misma fecha
                // (comparando solo la fecha, sin hora)
                LocalDateTime fechaInicio = reservaDTO.getFechaActividad().toLocalDate().atStartOfDay();
                LocalDateTime fechaFin = fechaInicio.plusDays(1);

                List<Reserva> reservasExistentes = reservaRepo.find(
                        "actividadId = ?1 AND fechaActividad >= ?2 AND fechaActividad < ?3 AND estado != ?4",
                        reservaDTO.getActividadId(), fechaInicio, fechaFin, "CANCELADA"
                ).list();

                // Sumar las personas ya reservadas
                int personasReservadas = reservasExistentes.stream()
                        .mapToInt(r -> r.getCantidadPersonas() != null ? r.getCantidadPersonas() : 0)
                        .sum();

                // Obtener capacidad máxima de la actividad
                int maxPeople = 20; // Valor por defecto
                try {
                    var actividad = actividadRestClient.findById(reservaDTO.getActividadId());
                    maxPeople = actividad.getMaxPersonas();
                    System.out.println("Capacidad máxima de actividad: " + maxPeople);
                } catch (Exception e) {
                    System.err.println("No se pudo obtener capacidad máxima, usando valor por defecto: " + maxPeople);
                }

                int personasSolicitadas = reservaDTO.getCantidadPersonas();
                int cupoDisponible = maxPeople - personasReservadas;

                System.out.println("Cupo: " + personasReservadas + "/" + maxPeople +
                        " reservadas | Solicitadas: " + personasSolicitadas +
                        " | Disponible: " + cupoDisponible);

                if (personasSolicitadas > cupoDisponible) {
                    String mensaje;
                    if (cupoDisponible <= 0) {
                        mensaje = "No hay cupo disponible para esta fecha. La actividad está completa.";
                    } else {
                        mensaje = "Solo quedan " + cupoDisponible + " cupos disponibles para esta fecha. " +
                                "Solicitaste " + personasSolicitadas + " personas.";
                    }

                    return Response.status(Response.Status.CONFLICT)
                            .entity(Map.of(
                                    "error", mensaje,
                                    "cupoDisponible", cupoDisponible,
                                    "personasReservadas", personasReservadas,
                                    "maxPeople", maxPeople
                            )).build();
                }
            }

            // VALIDACIONES OPCIONALES CON MANEJO DE ERRORES MEJORADO
            boolean skipExternalValidation = false;

            // Validar usuario (opcional pero recomendado)
            try {
                System.out.println("Validando usuario ID: " + userId);
                var usuario = usuarioRestClient.findById(userId);
                System.out.println("Usuario validado: " + usuario.getNombre());
            } catch (jakarta.ws.rs.WebApplicationException e) {
                System.err.println("Error al validar usuario - Status: " + e.getResponse().getStatus());

                if (e.getResponse().getStatus() == 404) {
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(Map.of("error", "Usuario no encontrado")).build();
                } else if (e.getResponse().getStatus() == 403) {
                    return Response.status(Response.Status.FORBIDDEN)
                            .entity(Map.of("error", "No tienes permisos para crear reservas")).build();
                } else if (e.getResponse().getStatus() == 500) {
                    System.err.println("Servicio de usuarios no disponible, continuando sin validación");
                    skipExternalValidation = true;
                } else {
                    System.err.println("Error inesperado del servicio de usuarios: " + e.getMessage());
                    skipExternalValidation = true;
                }
            } catch (Exception e) {
                System.err.println("Error de conectividad con servicio de usuarios: " + e.getMessage());
                skipExternalValidation = true;
            }

            // Validar actividad (opcional pero recomendado)
            try {
                System.out.println("Validando actividad ID: " + reservaDTO.getActividadId());
                var actividad = actividadRestClient.findById(reservaDTO.getActividadId());
                System.out.println("Actividad validada: " + actividad.getTitulo());
            } catch (jakarta.ws.rs.WebApplicationException e) {
                System.err.println("Error al validar actividad - Status: " + e.getResponse().getStatus());

                if (e.getResponse().getStatus() == 404) {
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(Map.of("error", "Actividad no encontrada")).build();
                } else if (e.getResponse().getStatus() == 500) {
                    System.err.println("Servicio de actividades no disponible, continuando sin validación");
                    skipExternalValidation = true;
                } else {
                    System.err.println("Error inesperado del servicio de actividades: " + e.getMessage());
                    skipExternalValidation = true;
                }
            } catch (Exception e) {
                System.err.println("Error de conectividad con servicio de actividades: " + e.getMessage());
                skipExternalValidation = true;
            }

            // Log si se saltaron validaciones externas
            if (skipExternalValidation) {
                System.out.println("Continuando creación de reserva sin todas las validaciones externas");
            }

            // Crear nueva reserva
            Reserva reserva = new Reserva();
            reserva.setActividadId(reservaDTO.getActividadId());
            reserva.setUsuarioId(userId);  // Del JWT
            reserva.setCantidadPersonas(reservaDTO.getCantidadPersonas());
            reserva.setCostoTotal(reservaDTO.getCostoTotal());
            reserva.setFechaActividad(reservaDTO.getFechaActividad());

            // Establecer fechas automáticamente
            LocalDateTime now = LocalDateTime.now();
            reserva.setFechaCreacion(now);
            reserva.setFechaActualizacion(now);
            reserva.setFechaReserva(now);

            // Estado por defecto
            reserva.setEstado("PENDIENTE");

            reserva.setId(null);
            reservaRepo.persist(reserva);

            System.out.println("Reserva creada exitosamente con ID: " + reserva.getId());

            return Response.status(Response.Status.CREATED)
                    .entity(Map.of(
                            "message", "Reserva creada exitosamente",
                            "reserva", convertToDTO(reserva),
                            "validacionesExternas", !skipExternalValidation
                    )).build();

        } catch (Exception e) {
            System.err.println("Error general al crear reserva: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                            "error", "Error interno del servidor",
                            "details", e.getMessage(),
                            "timestamp", LocalDateTime.now().toString()
                    )).build();
        }
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "CLIENTE"})
    public Response update(@PathParam("id") Integer id, ReservaDTO reservaDTO,
                           @Context SecurityContext securityContext) {
        try {
            Reserva obj = reservaRepo.findById(id);
            if (obj == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            // Verificar permisos: solo el propietario o ADMIN pueden actualizar
            if (!securityContext.isUserInRole("ADMIN")) {
                Integer userId = getUserIdFromJWT();
                if (!obj.getUsuarioId().equals(userId)) {
                    return Response.status(Response.Status.FORBIDDEN)
                            .entity("No tienes permiso para actualizar esta reserva")
                            .build();
                }
            }

            // Actualizar campos permitidos
            if (reservaDTO.getEstado() != null) {
                obj.setEstado(reservaDTO.getEstado());
            }
            if (reservaDTO.getFechaActividad() != null) {
                obj.setFechaActividad(reservaDTO.getFechaActividad());
            }
            if (reservaDTO.getCantidadPersonas() != null) {
                obj.setCantidadPersonas(reservaDTO.getCantidadPersonas());
            }
            if (reservaDTO.getCostoTotal() != null) {
                obj.setCostoTotal(reservaDTO.getCostoTotal());
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
    @RolesAllowed({"ADMIN", "CLIENTE"})
    public Response delete(@PathParam("id") Integer id, @Context SecurityContext securityContext) {
        try {
            Reserva reserva = reservaRepo.findById(id);
            if (reserva == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            // Verificar permisos
            if (!securityContext.isUserInRole("ADMIN")) {
                Integer userId = getUserIdFromJWT();
                if (!reserva.getUsuarioId().equals(userId)) {
                    return Response.status(Response.Status.FORBIDDEN)
                            .entity("No tienes permiso para eliminar esta reserva")
                            .build();
                }
            }

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
    @RolesAllowed({"ADMIN", "CLIENTE", "PROVEEDOR"})
    public Response findByUsuario(@PathParam("usuarioId") Integer usuarioId,
                                  @Context SecurityContext securityContext) {
        // Verificar permisos: solo el propietario o ADMIN pueden ver las reservas
        if (!securityContext.isUserInRole("ADMIN")) {
            Integer jwtUserId = getUserIdFromJWT();
            if (!usuarioId.equals(jwtUserId)) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity("No tienes permiso para ver las reservas de este usuario")
                        .build();
            }
        }

        var reservas = reservaRepo.find("usuarioId", usuarioId).list();
        List<ReservaDTO> reservasDTO = reservas.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return Response.ok(reservasDTO).build();
    }

    @GET
    @Path("/actividad/{actividadId}")
    @RolesAllowed({"ADMIN", "PROVEEDOR"})  // Solo ADMIN y PROVEEDOR pueden ver reservas por actividad
    public List<ReservaDTO> findByActividad(@PathParam("actividadId") Integer actividadId) {
        var reservas = reservaRepo.find("actividadId", actividadId).list();
        return reservas.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @GET
    @Path("/estado/{estado}")
    @RolesAllowed({"ADMIN"})  // Solo ADMIN puede filtrar por estado
    public List<ReservaDTO> findByEstado(@PathParam("estado") String estado) {
        var reservas = reservaRepo.find("estado", estado).list();
        return reservas.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @GET
    @Path("/mis-reservas")
    @RolesAllowed({"CLIENTE", "PROVEEDOR"})
    public List<ReservaDTO> getMisReservas() {
        Integer userId = getUserIdFromJWT();
        var reservas = reservaRepo.find("usuarioId", userId).list();
        return reservas.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Endpoint para cambiar estado de reserva
    @PUT
    @Path("/{id}/estado")
    @RolesAllowed({"ADMIN", "PROVEEDOR"})
    public Response cambiarEstado(@PathParam("id") Integer id,
                                  @QueryParam("nuevoEstado") String nuevoEstado,
                                  @Context SecurityContext securityContext) {
        try {
            Reserva reserva = reservaRepo.findById(id);
            if (reserva == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            // PROVEEDOR solo puede cambiar estado de sus propias actividades
            if (securityContext.isUserInRole("PROVEEDOR") && !securityContext.isUserInRole("ADMIN")) {
                // Aquí deberías validar que la actividad pertenece al proveedor
                // Por simplicidad, permitimos que cualquier proveedor cambie estados
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

    /**
     * Consultar disponibilidad de cupo para una actividad en una fecha.
     * Retorna cuántas personas ya reservaron y cuánto cupo queda.
     */
    @GET
    @Path("/disponibilidad/{actividadId}")
    @PermitAll
    public Response getDisponibilidad(
            @PathParam("actividadId") Integer actividadId,
            @QueryParam("fecha") String fechaStr) {
        try {
            if (fechaStr == null || fechaStr.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "La fecha es requerida (formato: yyyy-MM-dd)"))
                        .build();
            }

            // Parsear la fecha
            java.time.LocalDate fecha = java.time.LocalDate.parse(fechaStr);
            LocalDateTime fechaInicio = fecha.atStartOfDay();
            LocalDateTime fechaFin = fechaInicio.plusDays(1);

            // Buscar reservas existentes para esa actividad y fecha (excluyendo canceladas)
            List<Reserva> reservasExistentes = reservaRepo.find(
                    "actividadId = ?1 AND fechaActividad >= ?2 AND fechaActividad < ?3 AND estado != ?4",
                    actividadId, fechaInicio, fechaFin, "CANCELADA"
            ).list();

            int personasReservadas = reservasExistentes.stream()
                    .mapToInt(r -> r.getCantidadPersonas() != null ? r.getCantidadPersonas() : 0)
                    .sum();

            // Obtener capacidad máxima
            int maxPeople = 20;
            try {
                var actividad = actividadRestClient.findById(actividadId);
                maxPeople = actividad.getMaxPersonas();
            } catch (Exception e) {
                System.err.println("No se pudo obtener capacidad máxima: " + e.getMessage());
            }

            int cupoDisponible = Math.max(0, maxPeople - personasReservadas);
            boolean disponible = cupoDisponible > 0;

            return Response.ok(Map.of(
                    "actividadId", actividadId,
                    "fecha", fechaStr,
                    "maxPeople", maxPeople,
                    "personasReservadas", personasReservadas,
                    "cupoDisponible", cupoDisponible,
                    "disponible", disponible,
                    "totalReservas", reservasExistentes.size()
            )).build();

        } catch (Exception e) {
            System.err.println("Error al consultar disponibilidad: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Error al consultar disponibilidad: " + e.getMessage()))
                    .build();
        }
    }

    // ===== MÉTODOS AUXILIARES =====

    private Integer getUserIdFromJWT() {
        try {
            Object userIdClaim = jwt.getClaim("userId");
            if (userIdClaim instanceof Number) {
                return ((Number) userIdClaim).intValue();
            } else if (userIdClaim instanceof String) {
                return Integer.valueOf((String) userIdClaim);
            } else {
                return Integer.valueOf(userIdClaim.toString());
            }
        } catch (Exception e) {
            System.err.println("Error al obtener userId del JWT: " + e.getMessage());
            throw new RuntimeException("Token JWT inválido");
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
        /*if (reserva.getHistorialReserva() != null) {
            dto.setHistorialReserva(reserva.getHistorialReserva());
        }*/

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
