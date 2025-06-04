package com.distribuida.rest;

import com.distribuida.clients.ReservaRestClient;
import com.distribuida.clients.UsuarioRestClient;
import com.distribuida.db.Actividad;
import com.distribuida.db.Galeria;
import com.distribuida.dtos.ActividadDTO;
import com.distribuida.dtos.GaleriaDTO;
import com.distribuida.repo.ActividadRepository;
import com.distribuida.repo.GaleriaRepository;
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

@Path("/actividades")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transactional
public class ActividadRest {

    @Inject
    private ActividadRepository actividadRepo;

    @Inject
    @RestClient
    UsuarioRestClient usuarioRestClient;

    @GET
    public List<ActividadDTO> findAll() {
        var actividades = actividadRepo.listAll();

        return actividades.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @GET
    @Path("/{id}")
    public Response findById(@PathParam("id") Integer id) {
        var op = actividadRepo.findByIdOptional(id);
        if (op.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        Actividad actividad = op.get();
        try {
            ActividadDTO dto = convertToDTO(actividad);
            return Response.ok(dto).build();
        } catch (Exception e) {
            System.err.println("Error al obtener información del usuario/proveedor: " + e.getMessage());
            // Si no se puede obtener la información del proveedor, devolver DTO básico
            ActividadDTO dto = convertToDTOBasic(actividad);
            return Response.ok(dto).build();
        }
    }

    @POST
    public Response create(Actividad actividad) {
        try {
            actividad.setId(null);

            // Establecer fechas automáticamente
            if (actividad.getFechaCreacion() == null) {
                actividad.setFechaCreacion(LocalDateTime.now());
            }
            if (actividad.getFechaActualizacion() == null) {
                actividad.setFechaActualizacion(LocalDateTime.now());
            }

            // Manejar galería si existe
            if (actividad.getGaleria() != null) {
                for (Galeria galeria : actividad.getGaleria()) {
                    galeria.setId(null);
                    galeria.setActividad(actividad);
                }
            }

            actividadRepo.persist(actividad);
            System.out.println("Actividad creada exitosamente con ID: " + actividad.getId());

            return Response.status(Response.Status.CREATED).entity(actividad).build();
        } catch (Exception e) {
            System.err.println("Error al crear actividad: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al crear actividad: " + e.getMessage()).build();
        }
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") Integer id, Actividad actividad) {
        try {
            Actividad obj = actividadRepo.findById(id);
            if (obj == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            obj.setTitulo(actividad.getTitulo());
            obj.setDescripcion(actividad.getDescripcion());
            obj.setUbicacionDestino(actividad.getUbicacionDestino());
            obj.setUbicacionSalida(actividad.getUbicacionSalida());
            obj.setPrecio(actividad.getPrecio());
            obj.setDuracion(actividad.getDuracion());
            obj.setNivelDificultad(actividad.getNivelDificultad());
            obj.setTipoActividad(actividad.getTipoActividad());
            obj.setDisponibilidad(actividad.getDisponibilidad());
            obj.setFechaActualizacion(LocalDateTime.now());

            // CORRECCIÓN: No cambies el proveedorId por actividad.getId()!
            if (actividad.getProveedorId() != null) {
                obj.setProveedorId(actividad.getProveedorId());
            }

            return Response.ok(obj).build();
        } catch (Exception e) {
            System.err.println("Error al actualizar actividad: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Integer id) {
        try {
            boolean deleted = actividadRepo.deleteById(id);
            if (!deleted) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            return Response.ok().build();
        } catch (Exception e) {
            System.err.println("Error al eliminar actividad: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Método auxiliar para convertir Actividad a ActividadDTO con información completa
    private ActividadDTO convertToDTO(Actividad actividad) {
        ActividadDTO dto = new ActividadDTO();

        // Mapear campos básicos
        dto.setId(actividad.getId());
        dto.setProveedorId(actividad.getProveedorId());
        dto.setTitulo(actividad.getTitulo());
        dto.setDescripcion(actividad.getDescripcion());
        dto.setUbicacionDestino(actividad.getUbicacionDestino());
        dto.setUbicacionSalida(actividad.getUbicacionSalida());
        dto.setTipoActividad(actividad.getTipoActividad());
        dto.setNivelDificultad(actividad.getNivelDificultad());
        dto.setPrecio(actividad.getPrecio());
        dto.setDuracion(actividad.getDuracion());
        dto.setDisponibilidad(actividad.getDisponibilidad());
        dto.setFechaCreacion(actividad.getFechaCreacion());
        dto.setFechaActualizacion(actividad.getFechaActualizacion());

        // Convertir galería
        if (actividad.getGaleria() != null) {
            List<GaleriaDTO> galeriaDTO = actividad.getGaleria().stream()
                    .map(this::convertGaleriaToDTO)
                    .collect(Collectors.toList());
            dto.setGaleria(galeriaDTO);
        }

        return dto;
    }

    // Método auxiliar para convertir sin llamadas externas
    private ActividadDTO convertToDTOBasic(Actividad actividad) {
        ActividadDTO dto = new ActividadDTO();

        dto.setId(actividad.getId());
        dto.setProveedorId(actividad.getProveedorId());
        dto.setTitulo(actividad.getTitulo());
        dto.setDescripcion(actividad.getDescripcion());
        dto.setUbicacionDestino(actividad.getUbicacionDestino());
        dto.setUbicacionSalida(actividad.getUbicacionSalida());
        dto.setTipoActividad(actividad.getTipoActividad());
        dto.setNivelDificultad(actividad.getNivelDificultad());
        dto.setPrecio(actividad.getPrecio());
        dto.setDuracion(actividad.getDuracion());
        dto.setDisponibilidad(actividad.getDisponibilidad());
        dto.setFechaCreacion(actividad.getFechaCreacion());
        dto.setFechaActualizacion(actividad.getFechaActualizacion());

        return dto;
    }

    // Método auxiliar para convertir Galeria a GaleriaDTO
    private GaleriaDTO convertGaleriaToDTO(Galeria galeria) {
        GaleriaDTO dto = new GaleriaDTO();
        dto.setId(galeria.getId());
        dto.setUrlFoto(galeria.getUrlFoto());
        // No incluir la actividad para evitar referencia circular
        return dto;
    }
}

