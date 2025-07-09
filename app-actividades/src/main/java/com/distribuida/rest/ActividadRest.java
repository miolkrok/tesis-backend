package com.distribuida.rest;

import com.distribuida.clients.UsuarioRestClient;
import com.distribuida.db.Actividad;
import com.distribuida.db.Galeria;
import com.distribuida.db.ServicioEvento;
import com.distribuida.dtos.*;
import com.distribuida.repo.ActividadRepository;
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

    @Inject
    JsonWebToken jwt;

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

    @POST
    @RolesAllowed({"PROVEEDOR", "ADMIN"})  // Solo proveedores pueden crear actividades
    public Response create(Actividad actividad) {
        try {
            Integer userId = getUserIdFromJWT();
            String userRole = getUserRoleFromJWT();

            System.out.println("Creando actividad para usuario: " + userId + " | Rol: " + userRole);

            actividad.setId(null);

            // Asignar automáticamente el usuario del JWT
            actividad.setUsuarioId(userId);

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

            // Manejar servicios evento si existe
            if (actividad.getServicioEvento() != null) {
                for (ServicioEvento servicio : actividad.getServicioEvento()) {
                    servicio.setId(null);
                    servicio.setActividadServicio(actividad);
                }
            }

            actividadRepo.persist(actividad);
            System.out.println("Actividad creada exitosamente con ID: " + actividad.getId());

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
    public Response update(@PathParam("id") Integer id, Actividad actividad,
                           @Context SecurityContext securityContext) {
        try {
            Actividad obj = actividadRepo.findById(id);
            if (obj == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            // Verificar permisos: solo el propietario o ADMIN pueden actualizar
            if (!securityContext.isUserInRole("ADMIN")) {
                Integer userId = getUserIdFromJWT();
                if (!obj.getUsuarioId().equals(userId)) {
                    return Response.status(Response.Status.FORBIDDEN)
                            .entity("No tienes permiso para actualizar esta actividad")
                            .build();
                }
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

            return Response.ok(convertToDTO(obj)).build();
        } catch (Exception e) {
            System.err.println("Error al actualizar actividad: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
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

            // Verificar permisos: solo el propietario o ADMIN pueden eliminar
            if (!securityContext.isUserInRole("ADMIN")) {
                Integer userId = getUserIdFromJWT();
                if (!actividad.getUsuarioId().equals(userId)) {
                    return Response.status(Response.Status.FORBIDDEN)
                            .entity("No tienes permiso para eliminar esta actividad")
                            .build();
                }
            }

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

    // Método auxiliar para convertir Actividad a ActividadDTO con información completa
    private ActividadDTO convertToDTO(Actividad actividad) {
        ActividadDTO dto = new ActividadDTO();

        // Mapear campos básicos
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

        // Convertir galería
        if (actividad.getGaleria() != null) {
            List<GaleriaDTO> galeriaDTO = actividad.getGaleria().stream()
                    .map(this::convertGaleriaToDTO)
                    .collect(Collectors.toList());
            dto.setGaleria(galeriaDTO);
        }

        // Convertir servicios evento
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
        dto.setActividadId(galeria.getActividad().getId());
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

