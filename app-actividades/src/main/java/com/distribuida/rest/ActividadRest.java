package com.distribuida.rest;

import com.distribuida.clients.BusquedaRestClient;
import com.distribuida.clients.UsuarioRestClient;
import com.distribuida.db.Actividad;
import com.distribuida.db.Galeria;
import com.distribuida.db.ServicioEvento;
import com.distribuida.dtos.*;
import com.distribuida.repo.ActividadRepository;
import com.distribuida.repo.GaleriaRepository;
import com.distribuida.repo.ServicioEventoRepository;
import com.distribuida.service.AzureBlobStorageService;
import com.distribuida.service.ImageService;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private GaleriaRepository galeriaRepo;

    @Inject
    private ServicioEventoRepository servicioEventoRepo;

    @Inject
    ImageService imageService;

    @Inject
    @RestClient
    UsuarioRestClient usuarioRestClient;

    @Inject
    JsonWebToken jwt;

    @Inject
    @RestClient
    BusquedaRestClient busquedaRestClient;

    @Inject
    private AzureBlobStorageService storageService;


    @GET
    @PermitAll  // Público para permitir búsqueda sin login
    public List<ActividadDTO> findAll() {
        var actividades = actividadRepo.listAll();

        return actividades.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @GET
    @Path("/{id}")
    @PermitAll  // Público para permitir ver detalles sin login
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
            ActividadDTO dto = convertToDTOBasic(actividad);
            return Response.ok(dto).build();
        }
    }

    /*@POST
    @PermitAll
    public Response create(Actividad actividad) {
        try {
            Integer userId = getUserIdFromJWT();
            String userRole = getUserRoleFromJWT();

            System.out.println("Creando actividad para usuario: " + userId + " | Rol: " + userRole);

            actividad.setId(null);
            actividad.setUsuarioId(userId);

            if (actividad.getFechaCreacion() == null) {
                actividad.setFechaCreacion(LocalDateTime.now());
            }
            if (actividad.getFechaActualizacion() == null) {
                actividad.setFechaActualizacion(LocalDateTime.now());
            }

            // Manejar galería y servicios...
            if (actividad.getGaleria() != null) {
                for (Galeria galeria : actividad.getGaleria()) {
                    galeria.setId(null);
                    galeria.setActividad(actividad);
                }
            }

            // Separar galeria - se procesa despues
            List<Galeria> galeriaOriginal = actividad.getGaleria();
            actividad.setGaleria(null);


            if (actividad.getServicioEvento() != null) {
                for (ServicioEvento servicio : actividad.getServicioEvento()) {
                    servicio.setId(null);
                    servicio.setActividadServicio(actividad);
                }
            }

            //Guardar en la base de datos principal
            actividadRepo.persist(actividad);
            System.out.println("Actividad creada exitosamente con ID: " + actividad.getId());

            // Persistir solo galerias que ya tengan urlFoto
            if (galeriaOriginal != null) {
                for (Galeria galeria : galeriaOriginal) {
                    if (galeria.getUrlFoto() != null
                            && !galeria.getUrlFoto().isEmpty()) {
                        galeria.setId(null);
                        galeria.setActividad(actividad);
                        galeriaRepo.persist(galeria);
                    }
                }
            }


            // Sincronizar con el módulo de búsqueda
            try {
                sincronizarConBusqueda(actividad, "CREATE");
            } catch (Exception e) {
                System.err.println("Error al sincronizar con búsqueda: " + e.getMessage());
                // No fallar la creación si la sincronización falla
            }

            return Response.status(Response.Status.CREATED).entity(convertToDTO(actividad)).build();
        } catch (Exception e) {
            System.err.println("Error al crear actividad: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al crear actividad: " + e.getMessage()).build();
        }
    }*/

    @POST
    @PermitAll
    public Response create(ActividadDTO actividadDTO) {
        try {
            Integer userId = getUserIdFromJWT();
            String userRole = getUserRoleFromJWT();

            System.out.println("Creando actividad para usuario: " + userId + " | Rol: " + userRole);

            // Mapear DTO a entidad
            Actividad actividad = new Actividad();
            actividad.setUsuarioId(userId);
            actividad.setTitulo(actividadDTO.getTitulo());
            actividad.setDescripcion(actividadDTO.getDescripcion());
            actividad.setUbicacionDestino(actividadDTO.getUbicacionDestino());
            actividad.setUbicacionSalida(actividadDTO.getUbicacionSalida());
            actividad.setTipoActividad(actividadDTO.getTipoActividad());
            actividad.setNivelDificultad(actividadDTO.getNivelDificultad());
            actividad.setPrecio(actividadDTO.getPrecio());
            actividad.setDuracion(actividadDTO.getDuracion());
            actividad.setDisponibilidad(actividadDTO.getDisponibilidad());
            actividad.setMaximoPersonas(actividadDTO.getMaximoPersonas());
            actividad.setMinimoPersonas(actividadDTO.getMinimoPersonas());
            actividad.setEstadoActividad(actividadDTO.getEstadoActividad() != null ? actividadDTO.getEstadoActividad() : "ACTIVA");
            actividad.setFechaInicioDisponible(actividadDTO.getFechaInicioDisponible());
            actividad.setFechaFinDisponible(actividadDTO.getFechaFinDisponible());
            actividad.setProvincia(actividadDTO.getProvincia());
            actividad.setCiudad(actividadDTO.getCiudad());
            actividad.setLatitud(actividadDTO.getLatitud());
            actividad.setLongitud(actividadDTO.getLongitud());

            if (actividadDTO.getFechaCreacion() != null) {
                actividad.setFechaCreacion(actividadDTO.getFechaCreacion());
            } else {
                actividad.setFechaCreacion(LocalDateTime.now());
            }
            if (actividadDTO.getFechaActualizacion() != null) {
                actividad.setFechaActualizacion(actividadDTO.getFechaActualizacion());
            } else {
                actividad.setFechaActualizacion(LocalDateTime.now());
            }

            // Mapear servicios
            if (actividadDTO.getServicioEvento() != null) {
                List<ServicioEvento> servicios = new java.util.ArrayList<>();
                for (ServicioEventoDTO sDto : actividadDTO.getServicioEvento()) {
                    ServicioEvento servicio = new ServicioEvento();
                    servicio.setListaServicio(sDto.getListaServicio());
                    servicio.setActividadServicio(actividad);
                    servicios.add(servicio);
                }
                actividad.setServicioEvento(servicios);
            }

            // Persistir actividad SIN galería primero (necesitamos el ID)
            actividadRepo.persist(actividad);
            System.out.println("Actividad creada exitosamente con ID: " + actividad.getId());

            // Procesar galería: subir cada imagen a Azure y guardar urlFoto
            if (actividadDTO.getGaleria() != null && !actividadDTO.getGaleria().isEmpty()) {
                List<Galeria> galeriaList = new java.util.ArrayList<>();
                for (GaleriaDTO gDto : actividadDTO.getGaleria()) {
                    Galeria galeria = new Galeria();
                    galeria.setActividad(actividad);
                    galeria.setNombreArchivo(gDto.getNombreArchivo());
                    galeria.setTipoContenido(gDto.getTipoContenido());
                    galeria.setTamanoArchivo(gDto.getTamanoArchivo());
                    galeria.setEsImagenPrincipal(gDto.getEsImagenPrincipal());

                    // Si viene imagenBinaria (Base64), subir a Azure y obtener URL
                    if (gDto.getImagenBinaria() != null && !gDto.getImagenBinaria().isEmpty()) {
                        try {
                            storageService.validateImage(gDto.getImagenBinaria());
                            String imageUrl = storageService.uploadImageFromBase64(
                                    gDto.getImagenBinaria(),
                                    "actividades/" + actividad.getId(),
                                    gDto.getNombreArchivo()
                            );
                            galeria.setUrlFoto(imageUrl);
                            System.out.println("Imagen subida a Azure: " + imageUrl);
                        } catch (Exception e) {
                            System.err.println("Error al subir imagen: " + e.getMessage());
                            // Si falla el upload, usar urlFoto del DTO si existe
                            if (gDto.getUrlFoto() != null && !gDto.getUrlFoto().isEmpty()) {
                                galeria.setUrlFoto(gDto.getUrlFoto());
                            }
                        }
                    } else if (gDto.getUrlFoto() != null && !gDto.getUrlFoto().isEmpty()) {
                        // Si no viene Base64 pero sí viene URL directa
                        galeria.setUrlFoto(gDto.getUrlFoto());
                    }

                    // Solo persistir si tiene URL (imagen fue subida exitosamente)
                    if (galeria.getUrlFoto() != null && !galeria.getUrlFoto().isEmpty()) {
                        galeriaRepo.persist(galeria);
                        galeriaList.add(galeria);
                    }
                }
                actividad.setGaleria(galeriaList);
            }

            // Sincronizar con el módulo de búsqueda
            try {
                sincronizarConBusqueda(actividad, "CREATE");
            } catch (Exception e) {
                System.err.println("Error al sincronizar con búsqueda: " + e.getMessage());
                // No fallar la creación si la sincronización falla
            }

            return Response.status(Response.Status.CREATED).entity(convertToDTO(actividad)).build();
        } catch (Exception e) {
            System.err.println("Error al crear actividad: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al crear actividad: " + e.getMessage()).build();
        }
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({"PROVEEDOR", "ADMIN"})
    public Response update(@PathParam("id") Integer id,@Valid ActividadDTO actividadDTO,
                           @Context SecurityContext securityContext) {
        try {
            System.out.println("Actualizando actividad ID: " + id);

            Actividad obj = actividadRepo.findById(id);
            if (obj == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Actividad no encontrada")).build();
            }

            // Verificar permisos
            if (!securityContext.isUserInRole("ADMIN")) {
                Integer userId = getUserIdFromJWT();
                if (!obj.getUsuarioId().equals(userId)) {
                    return Response.status(Response.Status.FORBIDDEN)
                            .entity(Map.of("error", "No tienes permiso para actualizar esta actividad"))
                            .build();
                }
            }

            // === ACTUALIZAR CAMPOS BASICOS ===
            updateBasicFields(obj, actividadDTO);

            // === MANEJAR GALERIA ===
            if (actividadDTO.getGaleria() != null) {
                manejarGaleriaUpdate(obj, actividadDTO.getGaleria());
            }

            // === MANEJAR SERVICIOS ===
            if (actividadDTO.getServicioEvento() != null) {
                manejarServiciosUpdate(obj, actividadDTO.getServicioEvento());
            }

            obj.setFechaActualizacion(LocalDateTime.now());

            // Persistir cambios
            actividadRepo.persist(obj);

            // Sincronizar con modulo de búsqueda
            try {
                sincronizarConBusqueda(obj, "UPDATE");
            } catch (Exception e) {
                System.err.println("Error al sincronizar actualización con búsqueda: " + e.getMessage());
            }

            return Response.ok(convertToDTO(obj)).build();

        } catch (Exception e) {
            System.err.println("Error al actualizar actividad: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Error interno: " + e.getMessage())).build();
        }
    }

    /**
     * Actualiza los campos basicos de la actividad
     */
    private void updateBasicFields(Actividad obj, ActividadDTO actividadDTO) {
        if (actividadDTO.getTitulo() != null) {
            String titulo = actividadDTO.getTitulo().trim();
            if (titulo.isEmpty()) {
                throw new BadRequestException("El título no puede estar vacío");
            }
            if (titulo.length() < 3 || titulo.length() > 200) {
                throw new BadRequestException("El título debe tener entre 3 y 200 caracteres");
            }
            obj.setTitulo(titulo);
        }

        if (actividadDTO.getDescripcion() != null) {
            obj.setDescripcion(actividadDTO.getDescripcion());
        }
        if (actividadDTO.getUbicacionDestino() != null) {
            obj.setUbicacionDestino(actividadDTO.getUbicacionDestino());
        }
        if (actividadDTO.getUbicacionSalida() != null) {
            obj.setUbicacionSalida(actividadDTO.getUbicacionSalida());
        }
        if (actividadDTO.getPrecio() != null) {
            if (actividadDTO.getPrecio().compareTo(BigDecimal.ZERO) < 0) {
                throw new BadRequestException("El precio no puede ser negativo");
            }
            obj.setPrecio(actividadDTO.getPrecio());
        }
        if (actividadDTO.getDuracion() != null) {
            obj.setDuracion(actividadDTO.getDuracion());
        }
        if (actividadDTO.getNivelDificultad() != null) {
            obj.setNivelDificultad(actividadDTO.getNivelDificultad());
        }
        if (actividadDTO.getTipoActividad() != null) {
            obj.setTipoActividad(actividadDTO.getTipoActividad());
        }
        if (actividadDTO.getDisponibilidad() != null) {
            obj.setDisponibilidad(actividadDTO.getDisponibilidad());
        }
        if (actividadDTO.getFechaInicioDisponible() != null) {
            obj.setFechaInicioDisponible(actividadDTO.getFechaInicioDisponible());
        }
        if (actividadDTO.getFechaFinDisponible() != null) {
            obj.setFechaFinDisponible(actividadDTO.getFechaFinDisponible());
        }
        if (actividadDTO.getMinimoPersonas() != null) {
            obj.setMinimoPersonas(actividadDTO.getMinimoPersonas());
        }
        if (actividadDTO.getMaximoPersonas() != null) {
            obj.setMaximoPersonas(actividadDTO.getMaximoPersonas());
        }
        if (actividadDTO.getProvincia() != null) {
            obj.setProvincia(actividadDTO.getProvincia());
        }
        if (actividadDTO.getCiudad() != null) {
            obj.setCiudad(actividadDTO.getCiudad());
        }
        if (actividadDTO.getLatitud() != null) {
            if (actividadDTO.getLatitud() < -90 || actividadDTO.getLatitud() > 90) {
                throw new BadRequestException("La latitud debe estar entre -90 y 90");
            }
            obj.setLatitud(actividadDTO.getLatitud());
        }
        if (actividadDTO.getLongitud() != null) {
            obj.setLongitud(actividadDTO.getLongitud());
        }
        if (actividadDTO.getEstadoActividad() != null) {
            obj.setEstadoActividad(actividadDTO.getEstadoActividad());
        }

        // Validaciones de coherencia
        if (obj.getMinimoPersonas() != null && obj.getMaximoPersonas() != null) {
            if (obj.getMinimoPersonas() > obj.getMaximoPersonas()) {
                throw new BadRequestException("El mínimo de personas no puede ser mayor al máximo");
            }
        }

        if (obj.getFechaInicioDisponible() != null && obj.getFechaFinDisponible() != null) {
            if (obj.getFechaInicioDisponible().isAfter(obj.getFechaFinDisponible())) {
                throw new BadRequestException("La fecha de inicio no puede ser posterior a la fecha de fin");
            }
        }
    }

    /**
     * Maneja la actualización de la galería de forma inteligente
     * - Agrega nuevas imágenes (las que no tienen ID)
     * - Actualiza imágenes existentes (las que tienen ID)
     * - Mantiene las imágenes no enviadas en la petición
     */
    private void manejarGaleriaUpdate(Actividad actividad, List<GaleriaDTO> nuevaGaleria) {
        try {
            System.out.println("Actualizando galería para actividad: " + actividad.getId());

            for (GaleriaDTO galeriaDTO : nuevaGaleria) {
                if (galeriaDTO.getId() != null) {
                    // === SOLO ACTUALIZAR EXISTENTES ===
                    Galeria imagenExistente = galeriaRepo.findById(galeriaDTO.getId());
                    if (imagenExistente != null && imagenExistente.getActividad().getId().equals(actividad.getId())) {
                        // Verificar que pertenece a esta actividad (seguridad)
                        updateGaleriaFields(imagenExistente, galeriaDTO);

                        // Si es imagen principal, quitar bandera de otras
                        if (Boolean.TRUE.equals(galeriaDTO.getEsImagenPrincipal())) {
                            galeriaRepo.update("esImagenPrincipal = false WHERE actividad.id = ?1 AND id != ?2",
                                    actividad.getId(), galeriaDTO.getId());
                        }

                        galeriaRepo.persist(imagenExistente);
                        System.out.println("Imagen actualizada: " + galeriaDTO.getId());
                    } else {
                        System.err.println("Imagen no encontrada o no pertenece a esta actividad: " + galeriaDTO.getId());
                    }
                } else {
                    System.out.println("Skipping galería sin ID - usar endpoint específico para crear nuevas imágenes");
                }
            }

        } catch (Exception e) {
            System.err.println("Error al manejar galería: " + e.getMessage());
            throw new BadRequestException("Error procesando galería: " + e.getMessage());
        }
    }

    /**
     * Actualiza los campos de una imagen existente
     */
    private void updateGaleriaFields(Galeria galeria, GaleriaDTO dto) {
        if (dto.getUrlFoto() != null) {
            galeria.setUrlFoto(dto.getUrlFoto());
        }
        if (dto.getNombreArchivo() != null) {
            galeria.setNombreArchivo(dto.getNombreArchivo());
        }
        if (dto.getTipoContenido() != null) {
            galeria.setTipoContenido(dto.getTipoContenido());
        }
        if (dto.getTamanoArchivo() != null) {
            galeria.setTamanoArchivo(dto.getTamanoArchivo());
        }
        if (dto.getEsImagenPrincipal() != null) {
            galeria.setEsImagenPrincipal(dto.getEsImagenPrincipal());
        }

        // Actualizar imagen binaria si se proporciona
        if (dto.getImagenBinaria() != null
                && !dto.getImagenBinaria().isEmpty()) {
            storageService.validateImage(
                    dto.getImagenBinaria());

            String oldUrl = galeria.getUrlFoto();

            String newUrl = storageService
                    .uploadImageFromBase64(
                            dto.getImagenBinaria(),
                            "actividades/" + (galeria.getActividad()
                                    != null ? galeria.getActividad()
                                    .getId() : "temp"),
                            dto.getNombreArchivo());
            galeria.setUrlFoto(newUrl);
            if (oldUrl != null && !oldUrl.isEmpty()) {
                storageService.deleteImageByUrl(oldUrl);
            }
        }

    }


    /**
     * Maneja la actualización de servicios de forma inteligente
     * - Agrega nuevos servicios (los que no tienen ID)
     * - Actualiza servicios existentes (los que tienen ID)
     * - Mantiene los servicios no enviados en la petición
     */
    private void manejarServiciosUpdate(Actividad actividad, List<ServicioEventoDTO> nuevosServicios) {
        try {
            System.out.println("Actualizando servicios para actividad: " + actividad.getId());

            for (ServicioEventoDTO servicioDTO : nuevosServicios) {
                if (servicioDTO.getId() != null) {
                    // === SOLO ACTUALIZAR EXISTENTES ===
                    ServicioEvento servicioExistente = servicioEventoRepo.findById(servicioDTO.getId());
                    if (servicioExistente != null &&
                            servicioExistente.getActividadServicio().getId().equals(actividad.getId())) {
                        // Solo actualizar si pertenece a esta actividad (seguridad)
                        if (servicioDTO.getListaServicio() != null) {
                            servicioExistente.setListaServicio(servicioDTO.getListaServicio());
                        }

                        servicioEventoRepo.persist(servicioExistente);
                        System.out.println("Servicio actualizado: " + servicioDTO.getId());
                    } else {
                        System.err.println("Servicio no encontrado o no pertenece a esta actividad: " + servicioDTO.getId());
                    }
                } else {
                    System.out.println("Skipping servicio sin ID - usar endpoint específico para crear nuevos servicios");
                }
            }

        } catch (Exception e) {
            System.err.println("Error al manejar servicios: " + e.getMessage());
            throw new BadRequestException("Error procesando servicios: " + e.getMessage());
        }
    }


    @DELETE
    @Path("/{id}")
    @RolesAllowed({"PROVEEDOR", "ADMIN"})
    public Response delete(@PathParam("id") Integer id, @Context SecurityContext securityContext) {
        try {
            Actividad actividad = actividadRepo.findById(id);
            if (actividad == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            // Verificar permisos
            if (!securityContext.isUserInRole("ADMIN")) {
                Integer userId = getUserIdFromJWT();
                if (!actividad.getUsuarioId().equals(userId)) {
                    return Response.status(Response.Status.FORBIDDEN)
                            .entity("No tienes permiso para eliminar esta actividad")
                            .build();
                }
            }

            //Eliminar de la base principal
            boolean deleted = actividadRepo.deleteById(id);
            if (!deleted) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            //Sincronizar eliminación con búsqueda
            try {
                busquedaRestClient.eliminarDeBusqueda(id);
            } catch (Exception e) {
                System.err.println("Error al eliminar de búsqueda: " + e.getMessage());
            }

            return Response.ok().build();
        } catch (Exception e) {
            System.err.println("Error al eliminar actividad: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    private void sincronizarConBusqueda(Actividad actividad, String operacion) {
        try {
            // Crear el DTO para el módulo de búsqueda
            BusquedaActividadDTO busquedaDTO = new BusquedaActividadDTO();
            busquedaDTO.setActividadId(actividad.getId());
            busquedaDTO.setTitulo(actividad.getTitulo());
            busquedaDTO.setDescripcion(actividad.getDescripcion());
            busquedaDTO.setUbicacion(actividad.getUbicacionDestino());
            busquedaDTO.setCategoria(actividad.getTipoActividad());
            busquedaDTO.setPrecio(actividad.getPrecio());
            busquedaDTO.setDuracion(actividad.getDuracion());
            busquedaDTO.setTipoActividad(actividad.getTipoActividad());
            busquedaDTO.setNivelDificultad(actividad.getNivelDificultad());
            busquedaDTO.setProveedorId(actividad.getUsuarioId());
            busquedaDTO.setProvincia(actividad.getProvincia());
            busquedaDTO.setCiudad(actividad.getCiudad());
            busquedaDTO.setFechaInicioDisponible(actividad.getFechaInicioDisponible());
            busquedaDTO.setFechaFinDisponible(actividad.getFechaFinDisponible());
            busquedaDTO.setMinimoPersonas(actividad.getMinimoPersonas());
            busquedaDTO.setMaximoPersonas(actividad.getMaximoPersonas());
            busquedaDTO.setLatitud(actividad.getLatitud());
            busquedaDTO.setLongitud(actividad.getLongitud());
            busquedaDTO.setEstadoActividad(actividad.getEstadoActividad());
            busquedaDTO.setFechaIndexacion(LocalDateTime.now());

            // Obtener información del proveedor
            try {
                var usuario = usuarioRestClient.findById(actividad.getUsuarioId());
                if (usuario.getProveedor() != null) {
                    busquedaDTO.setNombreProveedor(usuario.getProveedor().getNombreEmpresa());
                }
            } catch (Exception e) {
                System.err.println("Error al obtener info del proveedor: " + e.getMessage());
            }

            // Enviar al módulo de búsqueda
            switch (operacion) {
                case "CREATE":
                    busquedaRestClient.indexarActividad(busquedaDTO);
                    break;
                case "UPDATE":
                    busquedaRestClient.actualizarIndice(actividad.getId(), busquedaDTO);
                    break;
            }

            System.out.println(" Actividad sincronizada con búsqueda: " + operacion + " - ID: " + actividad.getId());

        } catch (Exception e) {
            System.err.println(" Error en sincronización con búsqueda: " + e.getMessage());
            throw e; // Re-lanzar para que el caller maneje el error
        }
    }

    @GET
    @Path("/mis-actividades")
    @RolesAllowed({"PROVEEDOR"})
    public List<ActividadDTO> getMisActividades() {
        Integer userId = getUserIdFromJWT();
        var actividades = actividadRepo.find("usuarioId", userId).list();
        return actividades.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Búsqueda principal de actividades
     */
    @POST
    @Path("/buscar")
    @PermitAll
    public Response buscarActividades(@Valid BusquedaActividadRequest request) {
        try {
            System.out.println("Búsqueda de actividades: " + request);

            List<Actividad> actividades;

            // Determinar tipo de búsqueda
            if (request.getLatitud() != null && request.getLongitud() != null) {
                // Búsqueda por proximidad geográfica
                actividades = actividadRepo.buscarPorProximidad(
                        request.getLatitud(),
                        request.getLongitud(),
                        request.getRadioKm(),
                        request.getFechaInicio(),
                        request.getFechaFin(),
                        request.getCantidadPersonas()
                );
            } else {
                // Búsqueda tradicional
                actividades = actividadRepo.buscarActividadesDisponibles(
                        request.getUbicacion(),
                        request.getFechaInicio(),
                        request.getFechaFin(),
                        request.getCantidadPersonas(),
                        request.getTipoActividad(),
                        request.getPrecioMinimo(),
                        request.getPrecioMaximo()
                );
            }

            // Aplicar paginación manualmente (o usar Panache pagination)
            int inicio = request.getPagina() * request.getTamanoPagina();
            int fin = Math.min(inicio + request.getTamanoPagina(), actividades.size());

            List<Actividad> actividadesPaginadas = actividades.subList(
                    Math.min(inicio, actividades.size()),
                    fin
            );

            // Convertir a DTOs
            List<ActividadDTO> actividadesDTO = actividadesPaginadas.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            // Crear respuesta
            BusquedaActividadResponse response = new BusquedaActividadResponse();
            response.setActividades(actividadesDTO);
            response.setTotalElementos((long) actividades.size());
            response.setPaginaActual(request.getPagina());
            response.setElementosPorPagina(request.getTamanoPagina());
            response.setTotalPaginas((int) Math.ceil((double) actividades.size() / request.getTamanoPagina()));
            response.setHayMasPaginas(fin < actividades.size());

            // Agregar metadatos
            response.setProvinciasEncontradas(
                    actividades.stream()
                            .map(Actividad::getProvincia)
                            .filter(p -> p != null && !p.isEmpty())
                            .distinct()
                            .collect(Collectors.toList())
            );

            response.setTiposActividadEncontrados(
                    actividades.stream()
                            .map(Actividad::getTipoActividad)
                            .filter(t -> t != null && !t.isEmpty())
                            .distinct()
                            .collect(Collectors.toList())
            );

            response.setRangosPrecios(actividadRepo.obtenerRangosPrecios());

            return Response.ok(response).build();

        } catch (Exception e) {
            System.err.println("Error en búsqueda de actividades: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al buscar actividades: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Búsqueda rápida por texto
     */
    @GET
    @Path("/busqueda-rapida")
    @PermitAll
    public Response busquedaRapida(@QueryParam("q") String texto,
                                   @QueryParam("limite") @DefaultValue("10") Integer limite) {
        try {
            List<Actividad> actividades = actividadRepo.busquedaRapida(texto);

            List<ActividadDTO> actividadesDTO = actividades.stream()
                    .limit(limite)
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            return Response.ok(actividadesDTO).build();

        } catch (Exception e) {
            System.err.println("Error en búsqueda rápida: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Obtener filtros disponibles para búsqueda
     */
    @GET
    @Path("/filtros")
    @PermitAll
    public Response obtenerFiltros() {
        try {
            FiltrosBusquedaResponse filtros = new FiltrosBusquedaResponse();

            filtros.setProvincias(actividadRepo.obtenerProvinciasDisponibles());
            filtros.setTiposActividad(actividadRepo.obtenerTiposActividadDisponibles());
            filtros.setRangosPrecios(actividadRepo.obtenerRangosPrecios());
            filtros.setTotalActividades(actividadRepo.count("estadoActividad = 'ACTIVA'"));

            return Response.ok(filtros).build();

        } catch (Exception e) {
            System.err.println("Error al obtener filtros: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Obtener actividades populares
     */
    @GET
    @Path("/populares")
    @PermitAll
    public Response obtenerActividadesPopulares(@QueryParam("limite") @DefaultValue("10") Integer limite) {
        try {
            List<Actividad> actividades = actividadRepo.obtenerActividadesPopulares(limite);

            List<ActividadDTO> actividadesDTO = actividades.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            return Response.ok(actividadesDTO).build();

        } catch (Exception e) {
            System.err.println("Error al obtener actividades populares: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Sugerencias de búsqueda
     */
    @GET
    @Path("/sugerencias")
    @PermitAll
    public Response obtenerSugerencias(@QueryParam("texto") String texto) {
        try {
            SugerenciaBusquedaResponse sugerencias = new SugerenciaBusquedaResponse();

            // Sugerencias de ubicación
            if (texto != null && !texto.trim().isEmpty()) {
                String searchTerm = "%" + texto.toLowerCase() + "%";

                List<String> ubicaciones = actividadRepo.find(
                        "SELECT DISTINCT ubicacionDestino FROM Actividad WHERE LOWER(ubicacionDestino) LIKE ?1 AND estadoActividad = 'ACTIVA'",
                        searchTerm
                ).project(String.class).list();

                sugerencias.setSugerenciasUbicacion(ubicaciones.stream().limit(5).collect(Collectors.toList()));

                List<String> actividades = actividadRepo.find(
                        "SELECT DISTINCT tipoActividad FROM Actividad WHERE LOWER(tipoActividad) LIKE ?1 AND estadoActividad = 'ACTIVA'",
                        searchTerm
                ).project(String.class).list();

                sugerencias.setSugerenciasActividad(actividades.stream().limit(5).collect(Collectors.toList()));
            }

            // Actividades populares
            List<ActividadDTO> populares = actividadRepo.obtenerActividadesPopulares(5)
                    .stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            sugerencias.setActividadesPopulares(populares);

            return Response.ok(sugerencias).build();

        } catch (Exception e) {
            System.err.println("Error al obtener sugerencias: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Path("/con-rating")
    @PermitAll
    public Response findAllConRating() {
        try {
            var actividades = actividadRepo.listAll();

            List<Map<String, Object>> actividadesConRating = actividades.stream()
                    .map(actividad -> {
                        Map<String, Object> actividadMap = new HashMap<>();
                        actividadMap.put("id", actividad.getId());
                        actividadMap.put("titulo", actividad.getTitulo());
                        actividadMap.put("precio", actividad.getPrecio());
                        actividadMap.put("ubicacionDestino", actividad.getUbicacionDestino());

                        // Obtener rating
                        try {
                            // Aquí necesitarías inyectar un cliente REST para opiniones
                            // Por simplicidad, ponemos 0.0 por defecto
                            actividadMap.put("rating", 0.0);
                            actividadMap.put("totalOpiniones", 0);
                        } catch (Exception e) {
                            actividadMap.put("rating", 0.0);
                            actividadMap.put("totalOpiniones", 0);
                        }

                        // Obtener imagen principal
                        try {
                            GaleriaDTO imagenPrincipal = imageService.obtenerImagenPrincipal(actividad.getId());
                            actividadMap.put("imagenPrincipal", imagenPrincipal);
                        } catch (Exception e) {
                            actividadMap.put("imagenPrincipal", null);
                        }

                        return actividadMap;
                    })
                    .collect(Collectors.toList());

            return Response.ok(actividadesConRating).build();
        } catch (Exception e) {
            System.err.println("Error al obtener actividades con rating: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Agregar nueva imagen a una actividad existente
     */
    @POST
    @Path("/{id}/galeria")
    @RolesAllowed({"PROVEEDOR", "ADMIN"})
    public Response agregarImagen(@PathParam("id") Integer actividadId,
                                  @Valid GaleriaDTO galeriaDTO,
                                  @Context SecurityContext securityContext) {
        try {
            Actividad actividad = actividadRepo.findById(actividadId);
            if (actividad == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Actividad no encontrada")).build();
            }

            // Verificar permisos
            if (!securityContext.isUserInRole("ADMIN")) {
                Integer userId = getUserIdFromJWT();
                if (!actividad.getUsuarioId().equals(userId)) {
                    return Response.status(Response.Status.FORBIDDEN)
                            .entity(Map.of("error", "No tienes permiso para modificar esta actividad"))
                            .build();
                }
            }

            // Crear nueva imagen
            Galeria nuevaImagen = new Galeria();
            nuevaImagen.setUrlFoto(galeriaDTO.getUrlFoto());
            nuevaImagen.setNombreArchivo(galeriaDTO.getNombreArchivo());
            nuevaImagen.setTipoContenido(galeriaDTO.getTipoContenido());
            nuevaImagen.setTamanoArchivo(galeriaDTO.getTamanoArchivo());
            nuevaImagen.setEsImagenPrincipal(galeriaDTO.getEsImagenPrincipal());
            nuevaImagen.setActividad(actividad); // IMPORTANTE: Establecer relación

            // Subir imagen a Azure Blob Storage
            if (galeriaDTO.getImagenBinaria() != null
                    && !galeriaDTO.getImagenBinaria().isEmpty()) {
                storageService.validateImage(
                        galeriaDTO.getImagenBinaria());

                String imageUrl = storageService
                        .uploadImageFromBase64(
                                galeriaDTO.getImagenBinaria(),
                                "actividades/" + actividadId,
                                galeriaDTO.getNombreArchivo());
                nuevaImagen.setUrlFoto(imageUrl);
            }


            // Si es imagen principal, quitar bandera de otras
            if (Boolean.TRUE.equals(galeriaDTO.getEsImagenPrincipal())) {
                galeriaRepo.update("esImagenPrincipal = false WHERE actividad.id = ?1", actividadId);
            }

            galeriaRepo.persist(nuevaImagen);

            return Response.status(Response.Status.CREATED)
                    .entity(convertGaleriaToDTO(nuevaImagen)).build();

        } catch (Exception e) {
            System.err.println("Error al agregar imagen: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Error interno: " + e.getMessage())).build();
        }
    }

    /**
     * Agregar nuevo servicio a una actividad existente
     */
    @POST
    @Path("/{id}/servicios")
    @RolesAllowed({"PROVEEDOR", "ADMIN"})
    public Response agregarServicio(@PathParam("id") Integer actividadId,
                                    @Valid ServicioEventoDTO servicioDTO,
                                    @Context SecurityContext securityContext) {
        try {
            Actividad actividad = actividadRepo.findById(actividadId);
            if (actividad == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Actividad no encontrada")).build();
            }

            // Verificar permisos
            if (!securityContext.isUserInRole("ADMIN")) {
                Integer userId = getUserIdFromJWT();
                if (!actividad.getUsuarioId().equals(userId)) {
                    return Response.status(Response.Status.FORBIDDEN)
                            .entity(Map.of("error", "No tienes permiso para modificar esta actividad"))
                            .build();
                }
            }

            // Crear nuevo servicio
            ServicioEvento nuevoServicio = new ServicioEvento();
            nuevoServicio.setListaServicio(servicioDTO.getListaServicio());
            nuevoServicio.setActividadServicio(actividad); // IMPORTANTE: Establecer relación

            servicioEventoRepo.persist(nuevoServicio);

            return Response.status(Response.Status.CREATED)
                    .entity(convertServicioEventoToDTO(nuevoServicio)).build();

        } catch (Exception e) {
            System.err.println("Error al agregar servicio: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Error interno: " + e.getMessage())).build();
        }
    }

    /**
     * Eliminar imagen específica
     */
    @DELETE
    @Path("/{id}/galeria/{galeriaId}")
    @RolesAllowed({"PROVEEDOR", "ADMIN"})
    public Response eliminarImagen(@PathParam("id") Integer actividadId,
                                   @PathParam("galeriaId") Integer galeriaId,
                                   @Context SecurityContext securityContext) {
        try {
            Actividad actividad = actividadRepo.findById(actividadId);
            if (actividad == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Actividad no encontrada")).build();
            }

            // Verificar permisos
            if (!securityContext.isUserInRole("ADMIN")) {
                Integer userId = getUserIdFromJWT();
                if (!actividad.getUsuarioId().equals(userId)) {
                    return Response.status(Response.Status.FORBIDDEN)
                            .entity(Map.of("error", "No tienes permiso para modificar esta actividad"))
                            .build();
                }
            }

            Galeria galeria = galeriaRepo.findById(galeriaId);
            if (galeria == null || !galeria.getActividad().getId().equals(actividadId)) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Imagen no encontrada")).build();
            }

            boolean deleted = galeriaRepo.deleteById(galeriaId);
            if (!deleted) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            return Response.ok(Map.of("message", "Imagen eliminada exitosamente")).build();

        } catch (Exception e) {
            System.err.println("Error al eliminar imagen: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Error interno: " + e.getMessage())).build();
        }
    }

    /**
     * Eliminar servicio específico
     */
    @DELETE
    @Path("/{id}/servicios/{servicioId}")
    @RolesAllowed({"PROVEEDOR", "ADMIN"})
    public Response eliminarServicio(@PathParam("id") Integer actividadId,
                                     @PathParam("servicioId") Integer servicioId,
                                     @Context SecurityContext securityContext) {
        try {
            Actividad actividad = actividadRepo.findById(actividadId);
            if (actividad == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Actividad no encontrada")).build();
            }

            // Verificar permisos
            if (!securityContext.isUserInRole("ADMIN")) {
                Integer userId = getUserIdFromJWT();
                if (!actividad.getUsuarioId().equals(userId)) {
                    return Response.status(Response.Status.FORBIDDEN)
                            .entity(Map.of("error", "No tienes permiso para modificar esta actividad"))
                            .build();
                }
            }

            ServicioEvento servicio = servicioEventoRepo.findById(servicioId);
            if (servicio == null || !servicio.getActividadServicio().getId().equals(actividadId)) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("error", "Servicio no encontrado")).build();
            }

            boolean deleted = servicioEventoRepo.deleteById(servicioId);
            if (!deleted) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            return Response.ok(Map.of("message", "Servicio eliminado exitosamente")).build();

        } catch (Exception e) {
            System.err.println("Error al eliminar servicio: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Error interno: " + e.getMessage())).build();
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

    private String getUserRoleFromJWT() {
        try {
            return jwt.getGroups().iterator().next();
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    private ActividadDTO convertToDTO(Actividad actividad) {
        ActividadDTO dto = new ActividadDTO();

        // Mapear campos básicos (mantener tu código existente)
        dto.setId(actividad.getId());
        dto.setProveedorId(actividad.getUsuarioId());
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
        dto.setProvincia(actividad.getProvincia());
        dto.setCiudad(actividad.getCiudad());
        dto.setLatitud(actividad.getLatitud());
        dto.setLongitud(actividad.getLongitud());
        dto.setEstadoActividad(actividad.getEstadoActividad());
        dto.setFechaInicioDisponible(actividad.getFechaInicioDisponible());
        dto.setFechaFinDisponible(actividad.getFechaFinDisponible());
        dto.setMinimoPersonas(actividad.getMinimoPersonas());
        dto.setMaximoPersonas(actividad.getMaximoPersonas());

        // Convertir galería usando el método local
        if (actividad.getGaleria() != null) {
            List<GaleriaDTO> galeriaDTO = actividad.getGaleria().stream()
                    .map(this::convertGaleriaToDTO)
                    .collect(Collectors.toList());
            dto.setGaleria(galeriaDTO);
        }

        // Convertir servicios evento (mantener código existente)
        if (actividad.getServicioEvento() != null) {
            List<ServicioEventoDTO> serviciosDTO = actividad.getServicioEvento().stream()
                    .map(this::convertServicioEventoToDTO)
                    .collect(Collectors.toList());
            dto.setServicioEvento(serviciosDTO);
        }

        return dto;
    }

    // Método auxiliar para convertir sin llamadas externas
    private ActividadDTO convertToDTOBasic(Actividad actividad) {
        ActividadDTO dto = new ActividadDTO();

        dto.setId(actividad.getId());
        dto.setProveedorId(actividad.getUsuarioId());
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
        dto.setActividadId(galeria.getActividad() != null ? galeria.getActividad().getId() : null);
        dto.setNombreArchivo(galeria.getNombreArchivo());
        dto.setTipoContenido(galeria.getTipoContenido());
        dto.setTamanoArchivo(galeria.getTamanoArchivo());
        dto.setEsImagenPrincipal(galeria.getEsImagenPrincipal());

        return dto;
    }

    // Método auxiliar para convertir ServicioEvento a ServicioEventoDTO
    private ServicioEventoDTO convertServicioEventoToDTO(ServicioEvento servicioEvento) {
        ServicioEventoDTO dto = new ServicioEventoDTO();
        dto.setId(servicioEvento.getId());
        dto.setListaServicio(servicioEvento.getListaServicio());
        dto.setActividadId(servicioEvento.getActividadServicio().getId());
        return dto;
    }


}

