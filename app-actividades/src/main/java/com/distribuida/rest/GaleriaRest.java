package com.distribuida.rest;

import com.distribuida.db.Actividad;
import com.distribuida.db.Galeria;
import com.distribuida.dtos.GaleriaDTO;
import com.distribuida.repo.ActividadRepository;
import com.distribuida.repo.GaleriaRepository;
import com.distribuida.service.ImageService;
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

    @GET
    public List<GaleriaDTO> findAll() {
        System.out.println("findAll imagenes");
        return galeriaRepo.listAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @GET
    @Path("/{id}")
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
    public Response findByActividadId(@PathParam("actividadId") Integer actividadId) {
        System.out.println("findByActividadId: " + actividadId);
        List<GaleriaDTO> imagenes = obtenerImagenesPorActividad(actividadId);
        return Response.ok(imagenes).build();
    }

    @GET
    @Path("/actividad/{actividadId}/principal")
    public Response getImagenPrincipal(@PathParam("actividadId") Integer actividadId) {
        GaleriaDTO imagenPrincipal = obtenerImagenPrincipal(actividadId);
        if (imagenPrincipal == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(imagenPrincipal).build();
    }

    @POST
    public Response create(GaleriaDTO galeriaDTO) {
        try {
            // Validar que la actividad existe
            if (galeriaDTO.getActividadId() == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("El ID de la actividad es requerido").build();
            }

            Actividad actividad = actividadRepo.findById(galeriaDTO.getActividadId());
            if (actividad == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("La actividad especificada no existe").build();
            }

            // Convertir DTO a entidad
            Galeria galeria = convertToEntity(galeriaDTO);
            galeria.setId(null);
            galeria.setActividad(actividad);

            // Si es imagen principal, quitar la bandera de las otras
            if (Boolean.TRUE.equals(galeriaDTO.getEsImagenPrincipal())) {
                galeriaRepo.update(
                        "esImagenPrincipal = false WHERE actividad.id = ?1",
                        galeriaDTO.getActividadId()
                );
            }

            galeriaRepo.persist(galeria);
            System.out.println("Imagen creada exitosamente con ID: " + galeria.getId());

            return Response.status(Response.Status.CREATED)
                    .entity(convertToDTO(galeria)).build();
        } catch (Exception e) {
            System.err.println("Error al crear imagen: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al crear imagen: " + e.getMessage()).build();
        }
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") Integer id, GaleriaDTO galeriaDTO) {
        try {
            Galeria obj = galeriaRepo.findById(id);
            if (obj == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            // Actualizar campos
            obj.setUrlFoto(galeriaDTO.getUrlFoto());
            obj.setNombreArchivo(galeriaDTO.getNombreArchivo());
            obj.setTipoContenido(galeriaDTO.getTipoContenido());
            obj.setTamanoArchivo(galeriaDTO.getTamanoArchivo());
            obj.setEsImagenPrincipal(galeriaDTO.getEsImagenPrincipal());

            // Actualizar imagen binaria si se proporciona
            if (galeriaDTO.getImagenBinaria() != null && !galeriaDTO.getImagenBinaria().isEmpty()) {
                try {
                    byte[] imageBytes = Base64.getDecoder().decode(galeriaDTO.getImagenBinaria());
                    obj.setImagenBinaria(imageBytes);
                } catch (Exception e) {
                    System.err.println("Error al decodificar imagen: " + e.getMessage());
                }
            }

            // Actualizar actividad si se proporciona
            if (galeriaDTO.getActividadId() != null) {
                Actividad actividad = actividadRepo.findById(galeriaDTO.getActividadId());
                if (actividad != null) {
                    obj.setActividad(actividad);
                }
            }

            // Si es imagen principal, quitar la bandera de las otras
            if (Boolean.TRUE.equals(galeriaDTO.getEsImagenPrincipal())) {
                galeriaRepo.update(
                        "esImagenPrincipal = false WHERE actividad.id = ?1 AND id != ?2",
                        obj.getActividad().getId(), id
                );
            }

            return Response.ok(convertToDTO(obj)).build();
        } catch (Exception e) {
            System.err.println("Error al actualizar imagen: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Integer id) {
        try {
            boolean deleted = galeriaRepo.deleteById(id);
            if (!deleted) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.ok().build();
        } catch (Exception e) {
            System.err.println("Error al eliminar imagen: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Endpoint para subir imagen en multipart (alternativa)
     */
    @POST
    @Path("/upload/{actividadId}")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadImage(@PathParam("actividadId") Integer actividadId,
                                @FormParam("file") String fileBase64,
                                @FormParam("fileName") String fileName,
                                @FormParam("contentType") String contentType,
                                @FormParam("isPrincipal") @DefaultValue("false") Boolean isPrincipal) {
        try {
            GaleriaDTO galeriaDTO = new GaleriaDTO();
            galeriaDTO.setActividadId(actividadId);
            galeriaDTO.setImagenBinaria(fileBase64);
            galeriaDTO.setNombreArchivo(fileName);
            galeriaDTO.setTipoContenido(contentType);
            galeriaDTO.setEsImagenPrincipal(isPrincipal);

            return create(galeriaDTO);
        } catch (Exception e) {
            System.err.println("Error al subir imagen: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al subir imagen").build();
        }
    }

    // ===== MÉTODOS AUXILIARES =====

    private GaleriaDTO convertToDTO(Galeria galeria) {
        GaleriaDTO dto = new GaleriaDTO();
        dto.setId(galeria.getId());
        dto.setUrlFoto(galeria.getUrlFoto());
        dto.setActividadId(galeria.getActividad() != null ? galeria.getActividad().getId() : null);
        dto.setNombreArchivo(galeria.getNombreArchivo());
        dto.setTipoContenido(galeria.getTipoContenido());
        dto.setTamanoArchivo(galeria.getTamanoArchivo());
        dto.setEsImagenPrincipal(galeria.getEsImagenPrincipal());

        // Convertir imagen binaria a Base64 si existe
        if (galeria.getImagenBinaria() != null) {
            String base64Image = Base64.getEncoder().encodeToString(galeria.getImagenBinaria());
            dto.setImagenBinaria(base64Image);
        }

        return dto;
    }

    private Galeria convertToEntity(GaleriaDTO dto) {
        Galeria galeria = new Galeria();
        galeria.setId(dto.getId());
        galeria.setUrlFoto(dto.getUrlFoto());
        galeria.setNombreArchivo(dto.getNombreArchivo());
        galeria.setTipoContenido(dto.getTipoContenido());
        galeria.setTamanoArchivo(dto.getTamanoArchivo());
        galeria.setEsImagenPrincipal(dto.getEsImagenPrincipal());

        // Convertir imagen Base64 a binario si existe
        if (dto.getImagenBinaria() != null && !dto.getImagenBinaria().isEmpty()) {
            try {
                byte[] imageBytes = Base64.getDecoder().decode(dto.getImagenBinaria());
                galeria.setImagenBinaria(imageBytes);
            } catch (Exception e) {
                System.err.println("Error al decodificar imagen Base64: " + e.getMessage());
            }
        }

        return galeria;
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
            // Si no hay imagen principal, tomar la primera disponible
            imagenPrincipal = galeriaRepo.find(
                    "actividad.id = ?1",
                    actividadId
            ).firstResultOptional().orElse(null);
        }

        return imagenPrincipal != null ? convertToDTO(imagenPrincipal) : null;
    }


}
