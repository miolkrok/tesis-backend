package com.distribuida.rest;

import com.distribuida.clients.ReservaRestClient;
import com.distribuida.db.Pago;
import com.distribuida.dtos.PagoDTO;
import com.distribuida.repo.PagoRepository;
import com.distribuida.service.S3StorageService;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
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
    private S3StorageService s3StorageService;

    @Inject
    JsonWebToken jwt;



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
    @RolesAllowed({"CLIENTE", "ADMIN"})
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
                    s3StorageService.validateImage(pago.getImagenComprobante());

                    String s3Url = s3StorageService.uploadImageFromBase64(
                            pago.getImagenComprobante(),
                            "comprobantes/" + userId,
                            "comprobante_" + System.currentTimeMillis() + ".jpg"
                    );

                    pago.setImagenComprobante(s3Url); // GUARDAR URL DE S3
                    System.out.println("Comprobante subido a S3: " + s3Url);

                } catch (IllegalArgumentException e) {
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(Map.of("error", "Comprobante inválido: " + e.getMessage()))
                            .build();
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
                obj.setEstado(pago.getEstado());
            }

            // ACTUALIZAR COMPROBANTE EN S3 (si se envió uno nuevo)
            if (pago.getImagenComprobante() != null && !pago.getImagenComprobante().isEmpty()) {
                // Solo actualizar si es una imagen Base64 nueva (no URL)
                if (pago.getImagenComprobante().startsWith("data:image")) {
                    s3StorageService.validateImage(pago.getImagenComprobante());

                    String newUrl = s3StorageService.uploadImageFromBase64(
                            pago.getImagenComprobante(),
                            "comprobantes/" + obj.getUsuarioId(),
                            "comprobante_" + System.currentTimeMillis() + ".jpg"
                    );

                    obj.setImagenComprobante(newUrl);

                    // Eliminar comprobante anterior
                    if (oldComprobanteUrl != null && !oldComprobanteUrl.isEmpty()) {
                        s3StorageService.deleteImageByUrl(oldComprobanteUrl);
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
                s3StorageService.deleteImageByUrl(pago.getImagenComprobante());
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

        //Agregar URL del comprobante
        if (pago.getComprobante() != null) {
            dto.setComprobante(pago.getComprobante());
        }

        return dto;
    }
}

