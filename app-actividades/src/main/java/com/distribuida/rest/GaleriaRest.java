package com.distribuida.rest;

import com.distribuida.db.Actividad;
import com.distribuida.db.Galeria;
import com.distribuida.dtos.GaleriaDTO;
import com.distribuida.repo.ActividadRepository;
import com.distribuida.repo.GaleriaRepository;
import com.distribuida.service.AzureBlobStorageService;
import com.distribuida.service.ImageService;
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

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/imagenes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transactional
public class GaleriaRest {

    @Inject
    private GaleriaRepository galeriaRepo;

    @Inject
    private ActividadRepository actividadRepo;

    @Inject
    private AzureBlobStorageService storageService;

    @Inject
    JsonWebToken jwt;

    @GET
    @PermitAll
    public List<GaleriaDTO> findAll() {
        System.out.println("findAll imagenes");
        return galeriaRepo.listAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @GET
    @Path("/{id}")
    @PermitAll
    public Response findById(@PathParam("id") Integer id) {
        System.out.println("findById imagen: " + id);
        var op = galeriaRepo.findByIdOptional(id);
        if (op.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(convertToDTO(op.get())).build();
    }

    @GET
    @Path("/actividad/{actividadId}")
    @PermitAll
    public Response findByActividadId(@PathParam("actividadId") Integer actividadId) {
        System.out.println("findByActividadId: " + actividadId);
        List<GaleriaDTO> imagenes = obtenerImagenesPorActividad(actividadId);
        return Response.ok(imagenes).build();
    }

    @GET
    @Path("/actividad/{actividadId}/principal")
    @PermitAll
    public Response getImagenPrincipal(@PathParam("actividadId") Integer actividadId) {
        GaleriaDTO imagenPrincipal = obtenerImagenPrincipal(actividadId);
        if (imagenPrincipal == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(imagenPrincipal).build();
    }

    /**
     * NUEVO: Subir imagen a Azure y crear registro en galería
     */
    @POST
    @RolesAllowed({"CLIENTE","PROVEEDOR", "ADMIN"})
    public Response create(GaleriaDTO galeriaDTO,
                           @Context SecurityContext securityContext) {
        try {
            System.out.println("Iniciando subida de imagen a...");

            // Validar que la actividad existe
            if (galeriaDTO.getActividadId() == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "El ID de la actividad es requerido"))
                        .build();
            }

            Actividad actividad = actividadRepo.findById(galeriaDTO.getActividadId());
            if (actividad == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "La actividad especificada no existe"))
                        .build();
            }

            // Verificar permisos
            if (!securityContext.isUserInRole("ADMIN")) {
                Integer userId = getUserIdFromJWT();
                if (!actividad.getUsuarioId().equals(userId)) {
                    return Response.status(Response.Status.FORBIDDEN)
                            .entity(Map.of("error", "No tienes permiso para agregar imágenes a esta actividad"))
                            .build();
                }
            }

            // Validar que se envió una imagen
            if (galeriaDTO.getImagenBinaria() == null || galeriaDTO.getImagenBinaria().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "La imagen es requerida"))
                        .build();
            }

            // Validar imagen
            storageService.validateImage(galeriaDTO.getImagenBinaria());

            //SUBIR IMAGEN
            String urlAzure = storageService.uploadImageFromBase64(
                    galeriaDTO.getImagenBinaria(),
                    "actividades/" + galeriaDTO.getActividadId(),
                    galeriaDTO.getNombreArchivo()
            );

            System.out.println("Imagen subida a Azure: " + urlAzure);

            // Crear registro en base de datos
            Galeria galeria = new Galeria();
            galeria.setUrlFoto(urlAzure); // GUARDAR URL DE Azure
            galeria.setNombreArchivo(galeriaDTO.getNombreArchivo());
            galeria.setTipoContenido(galeriaDTO.getTipoContenido());
            galeria.setEsImagenPrincipal(galeriaDTO.getEsImagenPrincipal());
            galeria.setActividad(actividad);

            // Si es imagen principal, quitar la bandera de las otras
            if (Boolean.TRUE.equals(galeriaDTO.getEsImagenPrincipal())) {
                galeriaRepo.update(
                        "esImagenPrincipal = false WHERE actividad.id = ?1",
                        galeriaDTO.getActividadId()
                );
            }

            galeriaRepo.persist(galeria);
            System.out.println("Imagen guardada en BD con ID: " + galeria.getId());

            return Response.status(Response.Status.CREATED)
                    .entity(convertToDTO(galeria))
                    .build();

        } catch (IllegalArgumentException e) {
            System.err.println("Validación fallida: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        } catch (Exception e) {
            System.err.println("Error al crear imagen: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Error al subir imagen: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * ACTUALIZADO: Actualizar imagen (reemplazar en Azure)
     */
    @PUT
    @Path("/{id}")
    @RolesAllowed({"PROVEEDOR", "ADMIN"})
    public Response update(@PathParam("id") Integer id,
                           GaleriaDTO galeriaDTO,
                           @Context SecurityContext securityContext) {
        try {
            Galeria obj = galeriaRepo.findById(id);
            if (obj == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            // Verificar permisos
            if (!securityContext.isUserInRole("ADMIN")) {
                Integer userId = getUserIdFromJWT();
                if (!obj.getActividad().getUsuarioId().equals(userId)) {
                    return Response.status(Response.Status.FORBIDDEN)
                            .entity(Map.of("error", "No tienes permiso"))
                            .build();
                }
            }

            String oldUrl = obj.getUrlFoto();

            // Si se proporciona nueva imagen, subirla a Azure
            if (galeriaDTO.getImagenBinaria() != null && !galeriaDTO.getImagenBinaria().isEmpty()) {
                storageService.validateImage(galeriaDTO.getImagenBinaria());

                // Subir nueva imagen
                String newUrl = storageService.uploadImageFromBase64(
                        galeriaDTO.getImagenBinaria(),
                        "actividades/" + obj.getActividad().getId(),
                        galeriaDTO.getNombreArchivo()
                );

                obj.setUrlFoto(newUrl);

                // Eliminar imagen anterior de Azure
                if (oldUrl != null && !oldUrl.isEmpty()) {
                    storageService.deleteImageByUrl(oldUrl);
                }

                System.out.println("Imagen actualizada en Azure: " + newUrl);
            }

            // Actualizar otros campos
            if (galeriaDTO.getNombreArchivo() != null) {
                obj.setNombreArchivo(galeriaDTO.getNombreArchivo());
            }
            if (galeriaDTO.getTipoContenido() != null) {
                obj.setTipoContenido(galeriaDTO.getTipoContenido());
            }
            if (galeriaDTO.getEsImagenPrincipal() != null) {
                obj.setEsImagenPrincipal(galeriaDTO.getEsImagenPrincipal());

                if (Boolean.TRUE.equals(galeriaDTO.getEsImagenPrincipal())) {
                    galeriaRepo.update(
                            "esImagenPrincipal = false WHERE actividad.id = ?1 AND id != ?2",
                            obj.getActividad().getId(), id
                    );
                }
            }

            return Response.ok(convertToDTO(obj)).build();

        } catch (Exception e) {
            System.err.println("Error al actualizar imagen: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    /**
     * ACTUALIZADO: Eliminar imagen (también de Azure)
     */
    @DELETE
    @Path("/{id}")
    @RolesAllowed({"PROVEEDOR", "ADMIN"})
    public Response delete(@PathParam("id") Integer id,
                           @Context SecurityContext securityContext) {
        try {
            Galeria galeria = galeriaRepo.findById(id);
            if (galeria == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            // Verificar permisos
            if (!securityContext.isUserInRole("ADMIN")) {
                Integer userId = getUserIdFromJWT();
                if (!galeria.getActividad().getUsuarioId().equals(userId)) {
                    return Response.status(Response.Status.FORBIDDEN)
                            .entity(Map.of("error", "No tienes permiso"))
                            .build();
                }
            }

            // Eliminar de Azure
            if (galeria.getUrlFoto() != null && !galeria.getUrlFoto().isEmpty()) {
                storageService.deleteImageByUrl(galeria.getUrlFoto());
                System.out.println("Imagen eliminada de Azure: " + galeria.getUrlFoto());
            }

            // Eliminar de BD
            boolean deleted = galeriaRepo.deleteById(id);
            if (!deleted) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            return Response.ok(Map.of("message", "Imagen eliminada exitosamente")).build();

        } catch (Exception e) {
            System.err.println("Error al eliminar imagen: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
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
            throw new RuntimeException("Token JWT inválido");
        }
    }

    private GaleriaDTO convertToDTO(Galeria galeria) {
        GaleriaDTO dto = new GaleriaDTO();
        dto.setId(galeria.getId());
        dto.setUrlFoto(galeria.getUrlFoto()); // URL de Azure
        dto.setActividadId(galeria.getActividad() != null ? galeria.getActividad().getId() : null);
        dto.setNombreArchivo(galeria.getNombreArchivo());
        dto.setTipoContenido(galeria.getTipoContenido());
        dto.setTamanoArchivo(galeria.getTamanoArchivo());
        dto.setEsImagenPrincipal(galeria.getEsImagenPrincipal());

        // NO enviar binario de vuelta, solo la URL
        return dto;
    }

    private List<GaleriaDTO> obtenerImagenesPorActividad(Integer actividadId) {
        List<Galeria> imagenes = galeriaRepo.list("actividad.id", actividadId);
        return imagenes.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private GaleriaDTO obtenerImagenPrincipal(Integer actividadId) {
        Galeria imagenPrincipal = galeriaRepo.find(
                "actividad.id = ?1 AND esImagenPrincipal = true",
                actividadId
        ).firstResultOptional().orElse(null);

        if (imagenPrincipal == null) {
            imagenPrincipal = galeriaRepo.find(
                    "actividad.id = ?1",
                    actividadId
            ).firstResultOptional().orElse(null);
        }

        return imagenPrincipal != null ? convertToDTO(imagenPrincipal) : null;
    }

}
