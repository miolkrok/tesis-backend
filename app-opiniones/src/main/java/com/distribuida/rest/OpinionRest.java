package com.distribuida.rest;

import com.distribuida.clients.ActividadRestClient;
import com.distribuida.clients.UsuarioRestClient;
import com.distribuida.db.Opinion;
import com.distribuida.dtos.OpinionDTO;
import com.distribuida.repo.OpinionRepository;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.validation.Valid;
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

    @Inject
    JsonWebToken jwt;

    @GET
    @PermitAll
    public List<OpinionDTO> findAll() {
        var opiniones = opinionRepo.listAll();
        return opiniones.stream()
                .map(this::convertToDTOBasic) // Usar basic para evitar llamadas externas
                .collect(Collectors.toList());
    }

    @GET
    @Path("/{id}")
    @PermitAll
    public Response findById(@PathParam("id") Integer id) {
        System.out.println("Buscando opinión ID: " + id);
        var op = opinionRepo.findByIdOptional(id);
        if (op.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("Opinión no encontrada")
                    .build();
        }

        Opinion opinion = op.get();
        // Usar conversión básica para evitar problemas con REST clients
        OpinionDTO dto = convertToDTOBasic(opinion);
        return Response.ok(dto).build();
    }

    @POST
    @RolesAllowed({"CLIENTE", "ADMIN"})
    public Response create(@Valid OpinionDTO opinionDTO) {
        try {
            // Obtener userId del JWT automáticamente
            Integer userId = getUserIdFromJWT();
            System.out.println("Creando opinión para usuario: " + userId + " | Actividad: " + opinionDTO.getActividadId());

            // Validaciones básicas ANTES de las llamadas REST
            if (opinionDTO.getActividadId() == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("El ID de la actividad es requerido").build();
            }

            if (opinionDTO.getCalificacion() == null ||
                    opinionDTO.getCalificacion() < 1 || opinionDTO.getCalificacion() > 5) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("La calificación debe estar entre 1 y 5").build();
            }

            // Asignar automáticamente el usuario del JWT
            opinionDTO.setUsuarioId(userId);

            // Verificar que el usuario no haya opinado ya sobre esta actividad
            Opinion opinionExistente = opinionRepo.find(
                    "usuarioId = ?1 AND actividadId = ?2", userId, opinionDTO.getActividadId()
            ).firstResultOptional().orElse(null);

            if (opinionExistente != null) {
                return Response.status(Response.Status.CONFLICT)
                        .entity("Ya has opinado sobre esta actividad").build();
            }

            // VALIDACIONES OPCIONALES: Solo validar si es crítico
            // Comentar estas validaciones si los microservicios no están disponibles
            boolean validarExistencia = true; // Cambiar a false para omitir validaciones externas

            if (validarExistencia) {
                try {
                    System.out.println(" Validando usuario ID: " + userId);
                    var usuario = usuarioRestClient.findById(userId);
                    System.out.println(" Usuario validado: " + usuario.getNombre());
                } catch (Exception e) {
                    System.err.println(" Error al validar usuario: " + e.getMessage());
                    // OPCIÓN 1: Fallar si no se puede validar
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity("Error al validar usuario: " + e.getMessage()).build();

                    // OPCIÓN 2: Continuar sin validar (comentar return de arriba y descomentar esto)
                    // System.out.println(" Continuando sin validar usuario...");
                }

                try {
                    System.out.println("Validando actividad ID: " + opinionDTO.getActividadId());
                    var actividad = actividadRestClient.findById(opinionDTO.getActividadId());
                    System.out.println(" Actividad validada: " + actividad.getTitulo());
                } catch (Exception e) {
                    System.err.println(" Error al validar actividad: " + e.getMessage());
                    // OPCIÓN 1: Fallar si no se puede validar
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity("Error al validar actividad: " + e.getMessage()).build();

                    // OPCIÓN 2: Continuar sin validar (comentar return de arriba y descomentar esto)
                    // System.out.println(" Continuando sin validar actividad...");
                }
            }

            // Crear nueva opinión
            Opinion opinion = new Opinion();
            opinion.setActividadId(opinionDTO.getActividadId());
            opinion.setUsuarioId(userId);
            opinion.setCalificacion(opinionDTO.getCalificacion());
            opinion.setComentario(opinionDTO.getComentario());

            // Establecer fechas automáticamente
            LocalDateTime now = LocalDateTime.now();
            opinion.setFechaCreacion(now);
            opinion.setFechaActualizacion(now);

            opinion.setId(null);
            opinionRepo.persist(opinion);

            System.out.println(" Opinión creada exitosamente con ID: " + opinion.getId());
            return Response.status(Response.Status.CREATED).entity(convertToDTOBasic(opinion)).build();

        } catch (Exception e) {
            System.err.println(" Error general al crear opinión: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error interno: " + e.getMessage()).build();
        }
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({"CLIENTE", "ADMIN"})
    public Response update(@PathParam("id") Integer id, OpinionDTO opinionDTO,
                           @Context SecurityContext securityContext) {
        try {
            Opinion obj = opinionRepo.findById(id);
            if (obj == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Opinión no encontrada").build();
            }

            // Verificar permisos: solo el autor o ADMIN pueden actualizar
            if (!securityContext.isUserInRole("ADMIN")) {
                Integer userId = getUserIdFromJWT();
                if (!obj.getUsuarioId().equals(userId)) {
                    return Response.status(Response.Status.FORBIDDEN)
                            .entity("No tienes permiso para actualizar esta opinión")
                            .build();
                }
            }

            // Solo se permite actualizar calificación y comentario
            if (opinionDTO.getCalificacion() != null) {
                if (opinionDTO.getCalificacion() < 1 || opinionDTO.getCalificacion() > 5) {
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity("La calificación debe estar entre 1 y 5").build();
                }
                obj.setCalificacion(opinionDTO.getCalificacion());
            }

            if (opinionDTO.getComentario() != null) {
                obj.setComentario(opinionDTO.getComentario());
            }

            obj.setFechaActualizacion(LocalDateTime.now());

            return Response.ok(convertToDTOBasic(obj)).build();
        } catch (Exception e) {
            System.err.println("Error al actualizar opinión: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error interno: " + e.getMessage()).build();
        }
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({"CLIENTE", "ADMIN"})
    public Response delete(@PathParam("id") Integer id, @Context SecurityContext securityContext) {
        try {
            Opinion opinion = opinionRepo.findById(id);
            if (opinion == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Opinión no encontrada").build();
            }

            // Verificar permisos: solo el autor o ADMIN pueden eliminar
            if (!securityContext.isUserInRole("ADMIN")) {
                Integer userId = getUserIdFromJWT();
                if (!opinion.getUsuarioId().equals(userId)) {
                    return Response.status(Response.Status.FORBIDDEN)
                            .entity("No tienes permiso para eliminar esta opinión")
                            .build();
                }
            }

            boolean deleted = opinionRepo.deleteById(id);
            if (!deleted) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Opinión no encontrada").build();
            }

            return Response.ok()
                    .entity(Map.of("message", "Opinión eliminada exitosamente"))
                    .build();
        } catch (Exception e) {
            System.err.println("Error al eliminar opinión: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error interno: " + e.getMessage()).build();
        }
    }

    @GET
    @Path("/usuario/{usuarioId}")
    @RolesAllowed({"CLIENTE", "ADMIN", "PROVEEDOR"})
    public Response findByUsuario(@PathParam("usuarioId") Integer usuarioId,
                                  @Context SecurityContext securityContext) {
        // Verificar permisos: solo el propietario o ADMIN pueden ver las opiniones
        if (!securityContext.isUserInRole("ADMIN")) {
            Integer jwtUserId = getUserIdFromJWT();
            if (!usuarioId.equals(jwtUserId)) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity("No tienes permiso para ver las opiniones de este usuario")
                        .build();
            }
        }

        var opiniones = opinionRepo.find("usuarioId", usuarioId).list();
        List<OpinionDTO> opinionesDTO = opiniones.stream()
                .map(this::convertToDTOBasic)
                .collect(Collectors.toList());

        return Response.ok(opinionesDTO).build();
    }

    @GET
    @Path("/actividad/{actividadId}")
    @PermitAll
    public List<OpinionDTO> findByActividad(@PathParam("actividadId") Integer actividadId) {
        var opiniones = opinionRepo.find("actividadId", actividadId).list();
        return opiniones.stream()
                .map(this::convertToDTOBasic)
                .collect(Collectors.toList());
    }

    @GET
    @Path("/mis-opiniones")
    @RolesAllowed({"CLIENTE"})
    public List<OpinionDTO> getMisOpiniones() {
        Integer userId = getUserIdFromJWT();
        var opiniones = opinionRepo.find("usuarioId", userId).list();
        return opiniones.stream()
                .map(this::convertToDTOBasic)
                .collect(Collectors.toList());
    }

    @GET
    @Path("/promedio/actividad/{actividadId}")
    @PermitAll
    public Response getPromedioPuntuacion(@PathParam("actividadId") Integer actividadId) {
        var opiniones = opinionRepo.find("actividadId", actividadId).list();

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
                "promedioPuntuacion", Math.round(promedio * 100.0) / 100.0,
                "totalOpiniones", opiniones.size()
        )).build();
    }

    // Endpoint para probar conectividad
    @GET
    @Path("/test-connectivity")
    @PermitAll
    public Response testConnectivity() {
        Map<String, Object> result = Map.of(
                "opinionService", "OK",
                "timestamp", LocalDateTime.now().toString(),
                "totalOpiniones", opinionRepo.count()
        );

        // Probar conectividad con otros servicios
        try {
            var usuarios = usuarioRestClient.findAll();
            result = Map.of(
                    "opinionService", "OK",
                    "usuarioService", "OK - " + usuarios.size() + " usuarios",
                    "timestamp", LocalDateTime.now().toString(),
                    "totalOpiniones", opinionRepo.count()
            );
        } catch (Exception e) {
            result = Map.of(
                    "opinionService", "OK",
                    "usuarioService", "ERROR: " + e.getMessage(),
                    "timestamp", LocalDateTime.now().toString(),
                    "totalOpiniones", opinionRepo.count()
            );
        }

        try {
            var actividades = actividadRestClient.findAll();
            result = Map.of(
                    "opinionService", "OK",
                    "usuarioService", result.get("usuarioService"),
                    "actividadService", "OK - " + actividades.size() + " actividades",
                    "timestamp", LocalDateTime.now().toString(),
                    "totalOpiniones", opinionRepo.count()
            );
        } catch (Exception e) {
            result = Map.of(
                    "opinionService", "OK",
                    "usuarioService", result.get("usuarioService"),
                    "actividadService", "ERROR: " + e.getMessage(),
                    "timestamp", LocalDateTime.now().toString(),
                    "totalOpiniones", opinionRepo.count()
            );
        }

        return Response.ok(result).build();
    }

    // ===== MÉTODOS AUXILIARES =====

    private Integer getUserIdFromJWT() {
        try {
            Object userIdClaim = jwt.getClaim("userId");
            System.out.println(" Claim userId del JWT: " + userIdClaim + " (Tipo: " +
                    (userIdClaim != null ? userIdClaim.getClass().getSimpleName() : "null") + ")");

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

    private OpinionDTO convertToDTOBasic(Opinion opinion) {
        OpinionDTO dto = new OpinionDTO();
        dto.setId(opinion.getId());
        dto.setUsuarioId(opinion.getUsuarioId());
        dto.setActividadId(opinion.getActividadId());
        dto.setCalificacion(opinion.getCalificacion());
        dto.setComentario(opinion.getComentario());
        dto.setFechaCreacion(opinion.getFechaCreacion());
        dto.setFechaActualizacion(opinion.getFechaActualizacion());

        // Información básica sin llamadas REST
        dto.setNombreUsuario("Usuario " + opinion.getUsuarioId());
        dto.setTituloActividad("Actividad " + opinion.getActividadId());

        return dto;
    }

    // Método con llamadas REST (usar solo cuando los servicios estén disponibles)
    private OpinionDTO convertToDTO(Opinion opinion) {
        OpinionDTO dto = convertToDTOBasic(opinion);

        // Intentar obtener información adicional del usuario y actividad
        try {
            var usuario = usuarioRestClient.findById(opinion.getUsuarioId());
            dto.setNombreUsuario(usuario.getNombre() + " " + usuario.getApellido());
        } catch (Exception e) {
            System.err.println("No se pudo obtener info del usuario: " + e.getMessage());
            dto.setNombreUsuario("Usuario " + opinion.getUsuarioId());
        }

        try {
            var actividad = actividadRestClient.findById(opinion.getActividadId());
            dto.setTituloActividad(actividad.getTitulo());
        } catch (Exception e) {
            System.err.println("No se pudo obtener info de la actividad: " + e.getMessage());
            dto.setTituloActividad("Actividad " + opinion.getActividadId());
        }

        return dto;
    }
}
