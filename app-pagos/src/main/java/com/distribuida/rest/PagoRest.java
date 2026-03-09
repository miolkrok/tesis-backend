package com.distribuida.rest;

import com.distribuida.clients.ActividadRestClient;
import com.distribuida.clients.ReservaRestClient;
import com.distribuida.clients.UsuarioRestClient;
import com.distribuida.db.Pago;
import com.distribuida.dtos.ActividadDTO;
import com.distribuida.dtos.PagoDTO;
import com.distribuida.dtos.ReservaDTO;
import com.distribuida.dtos.UsuarioDTO;
import com.distribuida.repo.PagoRepository;
import com.distribuida.service.AzureBlobStorageService;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

@Path("/pagos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transactional
public class PagoRest {

    @Inject
    private PagoRepository pagoRepo;

    @Inject
    @RestClient
    ReservaRestClient reservaRestClient;

    @Inject
    private AzureBlobStorageService storageService;

    @Inject
    JsonWebToken jwt;

    @Inject
    @RestClient
    ActividadRestClient actividadRestClient;

    @Inject
    @RestClient
    UsuarioRestClient usuarioRestClient;

    /**
     * NUEVO: Obtener todos los pagos de las actividades de un anfitrión
     */
    @GET
    @Path("/anfitrion/{anfitrionId}")
    @RolesAllowed({"PROVEEDOR", "ADMIN"})
    public Response getPagosByAnfitrion(
            @PathParam("anfitrionId") Integer anfitrionId,
            @Context SecurityContext securityContext,
            @Context HttpHeaders headers) {

        try {
            System.out.println("=== getPagosByAnfitrion ===");
            System.out.println("anfitrionId: " + anfitrionId);

            // 1. Verificar permisos
            if (!securityContext.isUserInRole("ADMIN")) {
                Integer userId = getUserIdFromJWT();
                if (!userId.equals(anfitrionId)) {
                    return Response.status(Response.Status.FORBIDDEN).build();
                }
            }

            String authHeader = headers.getHeaderString("Authorization");

            // 2. Obtener actividades del anfitrión
            List<ActividadDTO> actividades = actividadRestClient
                    .findByUsuarioId(authHeader, anfitrionId);

            if (actividades.isEmpty()) {
                return Response.ok(List.of()).build();
            }

            // 3. Obtener IDs de actividades
            List<Integer> actividadIds = actividades.stream()
                    .map(ActividadDTO::getId)
                    .collect(Collectors.toList());

            // 4. Obtener reservas de esas actividades
            String actividadIdsStr = actividadIds.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            List<ReservaDTO> reservas = reservaRestClient.findByActividadIds(actividadIdsStr);

            if (reservas.isEmpty()) {
                return Response.ok(List.of()).build();
            }

            // 5. Obtener IDs de reservas
            List<Integer> reservaIds = reservas.stream()
                    .map(ReservaDTO::getId)
                    .collect(Collectors.toList());

            // 6. CONSULTA OPTIMIZADA: traer solo pagos de esas reservas
            List<Pago> pagos = pagoRepo.find(
                    "reservaId in ?1", reservaIds
            ).list();

            System.out.println("Pagos encontrados: " + pagos.size());

            // 7. Convertir a DTOs con información adicional
            List<PagoDTO> pagosDTO = pagos.stream().map(pago -> {
                PagoDTO dto = convertToDTO(pago);

                // Encontrar la reserva correspondiente
                reservas.stream()
                        .filter(r -> r.getId().equals(pago.getReservaId()))
                        .findFirst()
                        .ifPresent(reserva -> {
                            dto.setCantidadPersonas(reserva.getCantidadPersonas());
                            dto.setFechaReserva(reserva.getFechaReserva());

                            // Encontrar la actividad correspondiente
                            actividades.stream()
                                    .filter(a -> a.getId().equals(reserva.getActividadId()))
                                    .findFirst()
                                    .ifPresent(actividad -> {
                                        dto.setActividadTitulo(actividad.getTitulo());
                                    });

                            // Obtener información del usuario que hizo la reserva
                            try {
                                Map<String, Object> userMap = usuarioRestClient.findByIdPublic(reserva.getUsuarioId());
                                if (userMap != null) {
                                    Object nombre = userMap.get("nombre");
                                    Object apellido = userMap.get("apellido");
                                    String nombreCompleto = (nombre != null ? nombre.toString() : "") +
                                            (apellido != null ? " " + apellido.toString() : "");
                                    dto.setNombreUsuario(nombreCompleto.trim());
                                    dto.setEmailUsuario(userMap.get("email") != null ? userMap.get("email").toString() : null);
                                }
                            } catch (Exception e) {
                                System.err.println("Error al obtener usuario " + reserva.getUsuarioId() + ": " + e.getMessage());
                                dto.setNombreUsuario("Usuario #" + reserva.getUsuarioId());
                            }
                        });

                return dto;
            }).collect(Collectors.toList());

            return Response.ok(pagosDTO).build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError()
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    /**
     * NUEVO: Obtener pagos de una actividad específica
     */
    @GET
    @Path("/actividad/{actividadId}")
    @RolesAllowed({"PROVEEDOR", "ADMIN"})
    public Response getPagosByActividad(
            @PathParam("actividadId") Integer actividadId,
            @Context SecurityContext securityContext) {

        try {
            // Verificar que el usuario sea dueño de la actividad o admin
            if (!securityContext.isUserInRole("ADMIN")) {
                var actividad = actividadRestClient.findById(actividadId);
                if (actividad == null) {
                    return Response.status(Response.Status.NOT_FOUND).build();
                }

                Integer userId = getUserIdFromJWT();
                if (!actividad.getProveedorId().equals(userId)) {
                    return Response.status(Response.Status.FORBIDDEN)
                            .entity(Map.of("error", "No tienes permiso"))
                            .build();
                }
            }

            List<Pago> pagos = pagoRepo.findPagosByAnfitrionId(actividadId);

            List<PagoDTO> pagosDTO = pagos.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            return Response.ok(pagosDTO).build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @GET
    @RolesAllowed({"ADMIN"})
    public List<PagoDTO> findAll() {
        var pagos = pagoRepo.listAll();
        return pagos.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @GET
    @Path("/{id}")
    @PermitAll
    public Response findById(@PathParam("id") Integer id, @Context SecurityContext securityContext) {
        var op = pagoRepo.findByIdOptional(id);
        if (op.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        Pago pago = op.get();

        // Verificar permisos
        if (!securityContext.isUserInRole("ADMIN")) {
            Integer userId = getUserIdFromJWT();
            if (!pago.getUsuarioId().equals(userId)) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity(Map.of("error", "No tienes permiso"))
                        .build();
            }
        }

        return Response.ok(convertToDTO(pago)).build();
    }

    /**
     * ACTUALIZADO: Crear pago con comprobante en S3
     */
    @POST
    @RolesAllowed({"CLIENTE","PROVEEDOR", "ADMIN"})
    public Response create(Pago pago) {
        // Validar que exista la reserva
        try {
            System.out.println("Creando pago...");

            // Validar que exista la reserva
            try {
                reservaRestClient.findById(pago.getReservaId());
            } catch (Exception e) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Reserva no encontrada"))
                        .build();
            }

            // Establecer usuario del JWT
            Integer userId = getUserIdFromJWT();
            pago.setUsuarioId(userId);

            // Establecer fecha actual si no se proporcionó
            if (pago.getFechaTransaccion() == null) {
                pago.setFechaTransaccion(LocalDateTime.now());
            }
            pago.setFechaActualizacion(LocalDateTime.now());

            //SUBIR COMPROBANTE A S3 (si se envió)
            if (pago.getImagenComprobante() != null && !pago.getImagenComprobante().isEmpty()) {
                try {
                    String s3Url = storageService.uploadImageFromBase64(
                            pago.getImagenComprobante(),
                            "comprobantes/" + userId,
                            "comprobante_" + System.currentTimeMillis() + ".jpg"
                    );

                    pago.setImagenComprobante(s3Url); // GUARDAR URL DE S3
                    System.out.println("Comprobante subido a S3: " + s3Url);

                } catch (IllegalArgumentException e) {
                    System.err.println("Error al subir a S3: " + e.getMessage());
                }
            }

            pago.setId(null);
            pagoRepo.persist(pago);

            System.out.println("Pago creado con ID: " + pago.getId());
            return Response.status(Response.Status.CREATED)
                    .entity(convertToDTO(pago))
                    .build();

        } catch (Exception e) {
            System.err.println("Error al crear pago: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Error al crear pago: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * ACTUALIZADO: Actualizar pago (reemplazar comprobante si cambia)
     */
    @PUT
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "CLIENTE", "PROVEEDOR"})
    public Response update(@PathParam("id") Integer id, Pago pago, @Context SecurityContext securityContext) {
        try {
            Pago obj = pagoRepo.findById(id);
            if (obj == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            // Verificar permisos
            if (!securityContext.isUserInRole("ADMIN")) {
                Integer userId = getUserIdFromJWT();
                if (!obj.getUsuarioId().equals(userId)) {
                    return Response.status(Response.Status.FORBIDDEN)
                            .entity(Map.of("error", "No tienes permiso"))
                            .build();
                }
            }

            String oldComprobanteUrl = obj.getImagenComprobante();

            // Actualizar campos básicos
            if (pago.getMonto() != null) {
                obj.setMonto(pago.getMonto());
            }
            if (pago.getMetodoPago() != null) {
                obj.setMetodoPago(pago.getMetodoPago());
            }
            if (pago.getEstado() != null) {
                String nuevoEstado = pago.getEstado();
                obj.setEstado(nuevoEstado);

                // Si se rechaza el pago, cancelar la reserva asociada
                if ("RECHAZADO".equalsIgnoreCase(nuevoEstado)) {
                    try {
                        ReservaDTO reserva = reservaRestClient.findById(obj.getReservaId());
                        if (reserva != null) {
                            reserva.setEstado("CANCELADA");
                            reservaRestClient.update(obj.getReservaId(), reserva);
                            System.out.println("Reserva " + obj.getReservaId() + " cancelada por pago rechazado");
                        }
                    } catch (Exception e) {
                        System.err.println("Error al cancelar reserva: " + e.getMessage());
                    }
                }
            }

            // ACTUALIZAR COMPROBANTE EN S3 (si se envió uno nuevo)
            if (pago.getImagenComprobante() != null && !pago.getImagenComprobante().isEmpty()) {
                // Solo actualizar si es una imagen Base64 nueva (no URL)
                if (pago.getImagenComprobante().startsWith("data:image")) {
                    storageService.validateImage(pago.getImagenComprobante());

                    String newUrl = storageService.uploadImageFromBase64(
                            pago.getImagenComprobante(),
                            "comprobantes/" + obj.getUsuarioId(),
                            "comprobante_" + System.currentTimeMillis() + ".jpg"
                    );

                    obj.setImagenComprobante(newUrl);

                    // Eliminar comprobante anterior
                    if (oldComprobanteUrl != null && !oldComprobanteUrl.isEmpty()) {
                        storageService.deleteImageByUrl(oldComprobanteUrl);
                    }

                    System.out.println("Comprobante actualizado en S3: " + newUrl);
                }
            }

            obj.setFechaActualizacion(LocalDateTime.now());
            pagoRepo.persist(obj);

            return Response.ok(convertToDTO(obj)).build();

        } catch (Exception e) {
            System.err.println("Error al actualizar pago: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    /**
     * ACTUALIZADO: Eliminar pago (también elimina comprobante de S3)
     */
    @DELETE
    @Path("/{id}")
    @RolesAllowed({"ADMIN"})
    public Response delete(@PathParam("id") Integer id) {
        try {
            Pago pago = pagoRepo.findById(id);
            if (pago == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            // Eliminar comprobante de S3
            if (pago.getImagenComprobante() != null && !pago.getImagenComprobante().isEmpty()) {
                storageService.deleteImageByUrl(pago.getImagenComprobante());
                System.out.println("Comprobante eliminado de S3");
            }

            pagoRepo.deleteById(id);
            return Response.ok(Map.of("message", "Pago eliminado exitosamente")).build();

        } catch (Exception e) {
            System.err.println("Error al eliminar pago: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Path("/usuario/mis-pagos")
    @RolesAllowed({"CLIENTE", "PROVEEDOR"})
    public Response getMisPagos() {
        Integer userId = getUserIdFromJWT();
        var pagos = pagoRepo.find("usuarioId", userId).list();

        List<PagoDTO> pagosDTO = pagos.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return Response.ok(pagosDTO).build();
    }

    @GET
    @Path("/reserva/{reservaId}")
    public List<PagoDTO> findByReserva(@PathParam("reservaId") Integer reservaId) {
        var transacciones = pagoRepo.find("reservaId", reservaId).list();
        return transacciones.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

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
            throw new RuntimeException("Token JWT inválido");
        }
    }

    private PagoDTO convertToDTO(Pago pago) {
        PagoDTO dto = new PagoDTO();
        dto.setId(pago.getId());
        dto.setReservaId(pago.getReservaId());
        dto.setUsuarioId(pago.getUsuarioId());
        dto.setMonto(pago.getMonto());
        dto.setMetodoPago(pago.getMetodoPago());
        dto.setEstado(pago.getEstado());
        dto.setFechaTransaccion(pago.getFechaTransaccion());
        dto.setFechaActualizacion(pago.getFechaActualizacion());
        dto.setReembolso(pago.getReembolso());

        if (pago.getImagenComprobante() != null) {
            dto.setImagenComprobante(pago.getImagenComprobante());
        }

        dto.setActividadTitulo(null);
        dto.setCantidadPersonas(0);
        dto.setFechaReserva(null);
        dto.setNombreUsuario(null);
        dto.setEmailUsuario(null);

        return dto;
    }
}

