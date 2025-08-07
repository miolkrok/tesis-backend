package com.distribuida.rest;

import com.distribuida.clients.ActividadRestClient;
import com.distribuida.clients.GaleriaRestClient;
import com.distribuida.clients.OpinionRestClient;
import com.distribuida.db.Busqueda;
import com.distribuida.dtos.*;
import com.distribuida.repo.BusquedaRepository;
import com.distribuida.service.CoordenadasService;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;


@Path("/busquedas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transactional
public class BusquedaRest {

    @Inject
    BusquedaRepository busquedaRepository;

    @Inject
    @RestClient
    ActividadRestClient actividadRestClient;

    @Inject
    @RestClient
    GaleriaRestClient galeriaRestClient;


    @Inject
    @RestClient
    OpinionRestClient opinionRestClient;

    @Inject
    CoordenadasService coordenadasService;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public BusquedaRest() {
        // Actualizar indices cada 30 minutos
        scheduler.scheduleAtFixedRate(this::actualizarIndices, 1, 30, TimeUnit.MINUTES);
    }

    /**
     * Endpoint tipo "findAll" simplificado que devuelve todas las actividades
     * con solo los campos esenciales: id, titulo, imagen, precio, rating
     */
    @GET
    @Path("/simple")
    @PermitAll
    public Response findAllSimple() {
        try {
            System.out.println("Obteniendo todas las actividades - modo simple PARALELO");

            var actividades = actividadRestClient.findAll();

            // Usar parallelStream para procesar actividades en paralelo
            List<ActividadSimpleDTO> actividadesSimples = actividades.parallelStream()
                    .filter(actividad -> "ACTIVA".equals(actividad.getEstadoActividad()))
                    .map(this::convertirAActividadSimpleRapido)
                    .filter(Objects::nonNull) // Filtrar nulls en caso de errores
                    .collect(Collectors.toList());

            System.out.println("Procesadas " + actividadesSimples.size() + " actividades en modo paralelo");

            return Response.ok(actividadesSimples).build();

        } catch (Exception e) {
            System.err.println("Error en findAllSimple: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                            "error", "Error al obtener actividades",
                            "message", e.getMessage()
                    ))
                    .build();
        }
    }

    /*public Response findAllSimple() {
        try {
            System.out.println("Obteniendo todas las actividades - modo simple");

            var actividades = actividadRestClient.findAll();

            List<ActividadSimpleDTO> actividadesSimples = actividades.stream()
                    .filter(actividad -> "ACTIVA".equals(actividad.getEstadoActividad()))
                    .map(actividad -> {
                        ActividadSimpleDTO simple = new ActividadSimpleDTO();

                        // Datos básicos de la actividad
                        simple.setId(actividad.getId());
                        simple.setTitulo(actividad.getTitulo());
                        simple.setPrecio(actividad.getPrecio());

                        // Obtener imagen principal
                        try {
                            var responseImagen = galeriaRestClient.getImagenPrincipal(actividad.getId());
                            if (responseImagen.getStatus() == 200) {
                                Map<String, Object> imagenData = (Map<String, Object>) responseImagen.readEntity(Map.class);
                                if (imagenData != null && imagenData.get("imagenBinaria") != null) {
                                    simple.setImagen((String) imagenData.get("imagenBinaria"));
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("Error obteniendo imagen para actividad " + actividad.getId() + ": " + e.getMessage());
                            simple.setImagen(null);
                        }

                        // Obtener rating promedio
                        try {
                            var responseRating = opinionRestClient.getPromedioPuntuacion(actividad.getId());
                            if (responseRating.getStatus() == 200) {
                                Map<String, Object> ratingData = (Map<String, Object>) responseRating.readEntity(Map.class);
                                Object promedio = ratingData.get("promedioPuntuacion");

                                if (promedio instanceof Number) {
                                    double rating = ((Number) promedio).doubleValue();
                                    simple.setRating(Math.round(rating * 100.0) / 100.0); // Redondear a 2 decimales
                                } else {
                                    simple.setRating(0.0);
                                }
                            } else {
                                simple.setRating(0.0);
                            }
                        } catch (Exception e) {
                            System.err.println("Error obteniendo rating para actividad " + actividad.getId() + ": " + e.getMessage());
                            simple.setRating(0.0);
                        }

                        return simple;
                    })
                    .collect(Collectors.toList());

            System.out.println("Procesadas " + actividadesSimples.size() + " actividades en modo simple");

            return Response.ok(actividadesSimples).build();

        } catch (Exception e) {
            System.err.println("Error en findAllSimple: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                            "error", "Error al obtener actividades",
                            "message", e.getMessage()
                    ))
                    .build();
        }
    }*/


    /**
     * Busqueda principal con funcionalidad mejorada
     */
    @POST
    @Path("/buscar")
    public Response buscarActividades(@Valid BusquedaActividadRequestSimple request) {
        try {
            System.out.println("Busqueda de actividades: " + request);

            // Registrar busqueda para analytics
            registrarBusqueda(request);

            List<Busqueda> resultados;

            // Si hay coordenadas, usar busqueda por proximidad
            if (request.getLatitud() != null && request.getLongitud() != null) {
                resultados = buscarPorProximidad(request);
            } else if (request.getUbicacion() != null && !request.getUbicacion().trim().isEmpty()) {
                // Intentar geocodificar la ubicacion
                CoordenadasService.Coordenadas coords = coordenadasService.obtenerCoordenadas(request.getUbicacion());
                resultados = buscarPorProximidadConGeocodificacion(request, coords);
            } else {
                // Busqueda general
                resultados = buscarGeneral(request);
            }

            // Filtrar por disponibilidad de fechas si se especificaron
            if (request.getFechaInicio() != null && request.getFechaFin() != null) {
                resultados = filtrarPorDisponibilidadFechas(resultados, request.getFechaInicio(), request.getFechaFin());
            }

            // Filtrar por capacidad
            if (request.getCantidadPersonas() != null) {
                resultados = filtrarPorCapacidad(resultados, request.getCantidadPersonas());
            }

            // Convertir a respuesta paginada
            BusquedaActividadResponseSimple response = crearRespuestaPaginada(resultados, request);

            return Response.ok(response).build();

        } catch (Exception e) {
            System.err.println("Error en busqueda: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Error al realizar busqueda", "detalle", e.getMessage()))
                    .build();
        }
    }

    /**
     * Autocompletado de ubicaciones
     */
    @GET
    @Path("/ubicaciones/sugerencias")
    public Response obtenerSugerenciasUbicacion(@QueryParam("q") String texto) {
        try {
            if (texto == null || texto.trim().length() < 2) {
                return Response.ok(List.of()).build();
            }

            List<CoordenadasService.UbicacionSugerencia> sugerencias =
                    coordenadasService.obtenerSugerenciasUbicacion(texto);

            return Response.ok(sugerencias).build();

        } catch (Exception e) {
            System.err.println("Error obteniendo sugerencias: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Busqueda con filtros avanzados
     */
    @GET
    @Path("/filtros-avanzados")
    public List<Busqueda> buscarConFiltrosAvanzados(
            @QueryParam("ubicacion") String ubicacion,
            @QueryParam("fechaInicio") String fechaInicioStr,
            @QueryParam("fechaFin") String fechaFinStr,
            @QueryParam("personas") Integer cantidadPersonas,
            @QueryParam("tipo") String tipoActividad,
            @QueryParam("precioMin") BigDecimal precioMin,
            @QueryParam("precioMax") BigDecimal precioMax,
            @QueryParam("lat") Double latitud,
            @QueryParam("lng") Double longitud,
            @QueryParam("radio") @DefaultValue("50") Double radioKm) {

        LocalDate fechaInicio = fechaInicioStr != null ? LocalDate.parse(fechaInicioStr) : null;
        LocalDate fechaFin = fechaFinStr != null ? LocalDate.parse(fechaFinStr) : null;

        return busquedaRepository.buscarConFiltrosAvanzados(
                ubicacion, fechaInicio, fechaFin, cantidadPersonas,
                tipoActividad, precioMin, precioMax, latitud, longitud, radioKm
        );
    }

    /**
     * Obtener actividades cercanas a una ubicacion
     */
    @GET
    @Path("/cercanas")
    public Response obtenerActividadesCercanas(
            @QueryParam("lat") Double latitud,
            @QueryParam("lng") Double longitud,
            @QueryParam("radio") @DefaultValue("25") Double radioKm,
            @QueryParam("limite") @DefaultValue("10") Integer limite) {

        try {
            if (latitud == null || longitud == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Latitud y longitud son requeridas"))
                        .build();
            }

            List<Busqueda> resultados = busquedaRepository.buscarPorProximidad(
                    latitud, longitud, radioKm, limite
            );

            return Response.ok(resultados).build();

        } catch (Exception e) {
            System.err.println("Error buscando actividades cercanas: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Obtener filtros disponibles para busqueda
     */
    @GET
    @Path("/filtros-disponibles")
    public Response obtenerFiltrosDisponibles() {
        try {
            Map<String, Object> filtros = Map.of(
                    "provincias", busquedaRepository.obtenerProvinciasDisponibles(),
                    "tiposActividad", busquedaRepository.obtenerTiposActividadDisponibles(),
                    "rangosPrecios", busquedaRepository.obtenerRangosPrecios(),
                    "nivelesDificultad", busquedaRepository.obtenerNivelesDificultadDisponibles()
            );

            return Response.ok(filtros).build();

        } catch (Exception e) {
            System.err.println("Error obteniendo filtros: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }


    // ===== MeTODOS PRIVADOS DE APOYO =====

    private void registrarBusqueda(BusquedaActividadRequestSimple request) {
        try {
            // Aqui podrias guardar el historial de busquedas para analytics
            System.out.println("Registrando busqueda: " + request.getUbicacion() +
                    " - Personas: " + request.getCantidadPersonas() +
                    " - Fechas: " + request.getFechaInicio() + " a " + request.getFechaFin());
        } catch (Exception e) {
            System.err.println("Error registrando busqueda: " + e.getMessage());
        }
    }

    private List<Busqueda> buscarPorProximidad(BusquedaActividadRequestSimple request) {
        return busquedaRepository.buscarPorProximidad(
                request.getLatitud(),
                request.getLongitud(),
                request.getRadioKm() != null ? request.getRadioKm() : 50.0,
                20 // limite por defecto
        );
    }

    private List<Busqueda> buscarPorProximidadConGeocodificacion(
            BusquedaActividadRequestSimple request,
            CoordenadasService.Coordenadas coords) {

        return busquedaRepository.buscarPorProximidad(
                coords.getLatitud(),
                coords.getLongitud(),
                request.getRadioKm() != null ? request.getRadioKm() : 50.0,
                20
        );
    }

    private List<Busqueda> buscarGeneral(BusquedaActividadRequestSimple request) {
        return busquedaRepository.buscarConFiltros(
                request.getTextoBusqueda(),
                request.getTipoActividad(),
                request.getUbicacion(),
                request.getPrecioMinimo(),
                request.getPrecioMaximo()
        );
    }

    private List<Busqueda> filtrarPorDisponibilidadFechas(
            List<Busqueda> resultados,
            LocalDate fechaInicio,
            LocalDate fechaFin) {

        return resultados.stream()
                .filter(busqueda -> {
                    LocalDate inicioDisponible = busqueda.getFechaInicioDisponible();
                    LocalDate finDisponible = busqueda.getFechaFinDisponible();

                    return (inicioDisponible == null || !fechaInicio.isBefore(inicioDisponible)) &&
                            (finDisponible == null || !fechaFin.isAfter(finDisponible));
                })
                .toList();
    }

    private List<Busqueda> filtrarPorCapacidad(List<Busqueda> resultados, Integer cantidadPersonas) {
        return resultados.stream()
                .filter(busqueda -> {
                    Integer minimo = busqueda.getMinimoPersonas();
                    Integer maximo = busqueda.getMaximoPersonas();

                    return (minimo == null || cantidadPersonas >= minimo) &&
                            (maximo == null || cantidadPersonas <= maximo);
                })
                .toList();
    }

    private BusquedaActividadResponseSimple crearRespuestaPaginada(
            List<Busqueda> resultados,
            BusquedaActividadRequestSimple request) {

        int pagina = request.getPagina() != null ? request.getPagina() : 0;
        int tamanoPagina = request.getTamanoPagina() != null ? request.getTamanoPagina() : 20;

        int inicio = pagina * tamanoPagina;
        int fin = Math.min(inicio + tamanoPagina, resultados.size());

        List<Busqueda> resultadosPaginados = resultados.subList(
                Math.min(inicio, resultados.size()),
                fin
        );

        BusquedaActividadResponseSimple response = new BusquedaActividadResponseSimple();
        response.setActividades(resultadosPaginados);
        response.setTotalElementos((long) resultados.size());
        response.setPaginaActual(pagina);
        response.setElementosPorPagina(tamanoPagina);
        response.setTotalPaginas((int) Math.ceil((double) resultados.size() / tamanoPagina));
        response.setHayMasPaginas(fin < resultados.size());

        return response;
    }

    @Transactional
    public void actualizarIndices() {
        try {
            System.out.println("Iniciando actualizacion de indices de busqueda...");

            var actividades = actividadRestClient.findAll();
            int indexadas = 0;
            int errores = 0;

            for (var actividad : actividades) {
                try {
                    Busqueda indice = busquedaRepository.find("actividadId", actividad.getId())
                            .firstResultOptional()
                            .orElse(new Busqueda());

                    // Mapear datos basicos
                    indice.setActividadId(actividad.getId());
                    indice.setTitulo(actividad.getTitulo());
                    indice.setDescripcion(actividad.getDescripcion());
                    indice.setUbicacion(actividad.getUbicacionDestino());
                    indice.setCategoria(actividad.getTipoActividad());
                    indice.setPrecio(actividad.getPrecio());
                    indice.setDuracion(actividad.getDuracion());
                    indice.setTipoActividad(actividad.getTipoActividad());
                    indice.setNivelDificultad(actividad.getNivelDificultad());
                    indice.setProveedorId(actividad.getProveedorId());

                    // Nuevos campos
                    indice.setProvincia(actividad.getProvincia());
                    indice.setCiudad(actividad.getCiudad());
                    indice.setFechaInicioDisponible(actividad.getFechaInicioDisponible());
                    indice.setFechaFinDisponible(actividad.getFechaFinDisponible());
                    indice.setMinimoPersonas(actividad.getMinimoPersonas());
                    indice.setMaximoPersonas(actividad.getMaximoPersonas());
                    indice.setLatitud(actividad.getLatitud());
                    indice.setLongitud(actividad.getLongitud());
                    indice.setEstadoActividad(actividad.getEstadoActividad());
                    indice.setFechaIndexacion(LocalDateTime.now());

                    // Obtener promedio de opiniones
                    try {
                        var responseOpinion = opinionRestClient.getPromedioPuntuacion(actividad.getId());
                        indice.setPuntuacionPromedio(4.0); // Valor por defecto
                    } catch (Exception e) {
                        indice.setPuntuacionPromedio(0.0);
                    }

                    busquedaRepository.persistAndFlush(indice);
                    indexadas++;

                } catch (Exception e) {
                    errores++;
                    System.err.println("Error indexando actividad ID: " + actividad.getId() + ": " + e.getMessage());
                }
            }

            System.out.println("Indexacion completada: " + indexadas + " exitosas, " + errores + " errores");

        } catch (Exception e) {
            System.err.println("Error general en indexacion: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ===== MeTODOS HEREDADOS =====

    @GET
    public List<Busqueda> buscarGeneral(@QueryParam("q") String query) {
        if (query == null || query.trim().isEmpty()) {
            return busquedaRepository.listAll();
        }
        return busquedaRepository.buscar(query.trim());
    }

    @GET
    @Path("/categoria/{categoria}")
    public List<Busqueda> buscarPorCategoria(@PathParam("categoria") String categoria) {
        return busquedaRepository.buscarPorCategoria(categoria);
    }

    @GET
    @Path("/ubicacion/{ubicacion}")
    public List<Busqueda> buscarPorUbicacion(@PathParam("ubicacion") String ubicacion) {
        return busquedaRepository.buscarPorUbicacion(ubicacion);
    }

    @GET
    @Path("/indexar")
    public Response indexarActividades() {
        try {
            actualizarIndices();
            return Response.ok(Map.of(
                    "mensaje", "Indexacion completada exitosamente",
                    "timestamp", System.currentTimeMillis()
            )).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                            "error", "Error durante la indexacion",
                            "detalle", e.getMessage()
                    )).build();
        }
    }

    @GET
    @Path("/estadisticas")
    public Response obtenerEstadisticas() {
        try {
            long totalActividades = busquedaRepository.count();
            long actividadesConOpiniones = busquedaRepository.count("puntuacionPromedio > 0");

            Object[] precioStats = busquedaRepository.find("SELECT MIN(precio), MAX(precio), AVG(precio) FROM Busqueda")
                    .project(Object[].class)
                    .firstResult();

            return Response.ok(Map.of(
                    "totalActividades", totalActividades,
                    "actividadesConOpiniones", actividadesConOpiniones,
                    "precioMinimo", precioStats != null ? precioStats[0] : 0,
                    "precioMaximo", precioStats != null ? precioStats[1] : 0,
                    "precioPromedio", precioStats != null ? precioStats[2] : 0,
                    "ultimaActualizacion", System.currentTimeMillis()
            )).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Error al obtener estadisticas"))
                    .build();
        }
    }



    /**
     * Endpoint para indexar nueva actividad desde el modulo de actividades
     */
    @POST
    @Path("/indexar")
    @RolesAllowed({"PROVEEDOR", "ADMIN"})
    public Response indexarActividad(BusquedaActividadDTO actividadDTO) {
        try {
            System.out.println("Indexando nueva actividad: " + actividadDTO.getActividadId());

            // Convertir DTO a entidad Busqueda
            Busqueda busqueda = new Busqueda();
            mapearDTOaBusqueda(actividadDTO, busqueda);

            // Persistir en base de busqueda
            busquedaRepository.persist(busqueda);

            System.out.println("Actividad indexada exitosamente: " + actividadDTO.getActividadId());
            return Response.status(Response.Status.CREATED)
                    .entity(Map.of("message", "Actividad indexada exitosamente"))
                    .build();

        } catch (Exception e) {
            System.err.println(" Error al indexar actividad: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Error al indexar actividad"))
                    .build();
        }
    }

    /**
     * Endpoint para actualizar indice existente
     */
    @PUT
    @Path("/indexar/{actividadId}")
    @RolesAllowed({"PROVEEDOR", "ADMIN"})
    public Response actualizarIndice(@PathParam("actividadId") Integer actividadId,
                                     BusquedaActividadDTO actividadDTO) {
        try {
            System.out.println("Actualizando indice para actividad: " + actividadId);

            // Buscar registro existente
            Busqueda busqueda = busquedaRepository.find("actividadId", actividadId)
                    .firstResultOptional()
                    .orElse(new Busqueda());

            // Actualizar datos
            mapearDTOaBusqueda(actividadDTO, busqueda);
            busqueda.setFechaIndexacion(LocalDateTime.now());

            // Persistir cambios
            busquedaRepository.persist(busqueda);

            System.out.println("indice actualizado exitosamente: " + actividadId);
            return Response.ok(Map.of("message", "indice actualizado exitosamente")).build();

        } catch (Exception e) {
            System.err.println(" Error al actualizar indice: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Error al actualizar indice"))
                    .build();
        }
    }

    /**
     * Endpoint para eliminar de indice
     */
    @DELETE
    @Path("/indexar/{actividadId}")
    @RolesAllowed({"PROVEEDOR", "ADMIN"})
    public Response eliminarDeBusqueda(@PathParam("actividadId") Integer actividadId) {
        try {
            System.out.println("Eliminando de indice actividad: " + actividadId);

            // Eliminar todos los registros con esa actividad
            long eliminados = busquedaRepository.delete("actividadId", actividadId);

            System.out.println("Registros eliminados del indice: " + eliminados);
            return Response.ok(Map.of(
                    "message", "Actividad eliminada del indice",
                    "registrosEliminados", eliminados
            )).build();

        } catch (Exception e) {
            System.err.println("Error al eliminar de indice: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Error al eliminar de indice"))
                    .build();
        }
    }

    /**
     * Reindexar todas las actividades desde el modulo principal
     */
    @POST
    @Path("/reindexar")
    @RolesAllowed({"ADMIN"})
    public Response reindexarTodo() {
        try {
            System.out.println("Iniciando reindexacion completa...");

            // Limpiar indice actual
            busquedaRepository.deleteAll();

            // Obtener todas las actividades del modulo principal
            var actividades = actividadRestClient.findAll();
            int indexadas = 0;
            int errores = 0;

            for (var actividad : actividades) {
                try {
                    // Crear registro de busqueda
                    Busqueda busqueda = new Busqueda();
                    busqueda.setActividadId(actividad.getId());
                    busqueda.setTitulo(actividad.getTitulo());
                    busqueda.setDescripcion(actividad.getDescripcion());
                    busqueda.setUbicacion(actividad.getUbicacionDestino());
                    busqueda.setCategoria(actividad.getTipoActividad());
                    busqueda.setPrecio(actividad.getPrecio());
                    busqueda.setDuracion(actividad.getDuracion());
                    busqueda.setTipoActividad(actividad.getTipoActividad());
                    busqueda.setNivelDificultad(actividad.getNivelDificultad());
                    busqueda.setProveedorId(actividad.getProveedorId());
                    busqueda.setProvincia(actividad.getProvincia());
                    busqueda.setCiudad(actividad.getCiudad());
                    busqueda.setFechaInicioDisponible(actividad.getFechaInicioDisponible());
                    busqueda.setFechaFinDisponible(actividad.getFechaFinDisponible());
                    busqueda.setMinimoPersonas(actividad.getMinimoPersonas());
                    busqueda.setMaximoPersonas(actividad.getMaximoPersonas());
                    busqueda.setLatitud(actividad.getLatitud());
                    busqueda.setLongitud(actividad.getLongitud());
                    busqueda.setEstadoActividad(actividad.getEstadoActividad());
                    busqueda.setFechaIndexacion(LocalDateTime.now());

                    busquedaRepository.persist(busqueda);
                    indexadas++;

                } catch (Exception e) {
                    errores++;
                    System.err.println("Error indexando actividad ID: " + actividad.getId() + ": " + e.getMessage());
                }
            }

            System.out.println("Reindexacion completada: " + indexadas + " exitosas, " + errores + " errores");

            return Response.ok(Map.of(
                    "message", "Reindexacion completada",
                    "actividadesIndexadas", indexadas,
                    "errores", errores
            )).build();

        } catch (Exception e) {
            System.err.println("Error en reindexacion: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Error en reindexacion"))
                    .build();
        }
    }

    /**
     * Metodo auxiliar para mapear DTO a entidad Busqueda
     */
    private void mapearDTOaBusqueda(BusquedaActividadDTO dto, Busqueda busqueda) {
        busqueda.setActividadId(dto.getActividadId());
        busqueda.setTitulo(dto.getTitulo());
        busqueda.setDescripcion(dto.getDescripcion());
        busqueda.setUbicacion(dto.getUbicacion());
        busqueda.setCategoria(dto.getCategoria());
        busqueda.setPrecio(dto.getPrecio()); //!= null ? BigDecimal.valueOf(dto.getPrecio().byteValueExact()) : null);
        busqueda.setDuracion(dto.getDuracion());
        busqueda.setTipoActividad(dto.getTipoActividad());
        busqueda.setNivelDificultad(dto.getNivelDificultad());
        busqueda.setProveedorId(dto.getProveedorId());
        busqueda.setNombreProveedor(dto.getNombreProveedor());
        busqueda.setProvincia(dto.getProvincia());
        busqueda.setCiudad(dto.getCiudad());
        busqueda.setFechaInicioDisponible(dto.getFechaInicioDisponible());
        busqueda.setFechaFinDisponible(dto.getFechaFinDisponible());
        busqueda.setMinimoPersonas(dto.getMinimoPersonas());
        busqueda.setMaximoPersonas(dto.getMaximoPersonas());
        busqueda.setLatitud(dto.getLatitud());
        busqueda.setLongitud(dto.getLongitud());
        busqueda.setEstadoActividad(dto.getEstadoActividad());
        busqueda.setFechaIndexacion(LocalDateTime.now());
        busqueda.setPuntuacionPromedio(0.0); // Valor por defecto
        busqueda.setNumeroReservas(0);
        busqueda.setNumeroOpiniones(0);
    }

    /**
     * Método auxiliar para convertir Busqueda a ActividadBusquedaSimpleDTO
     * con llamadas a los microservicios para obtener imagen y rating
     */
    private ActividadSimpleDTO convertirAActividadSimpleRapido(ActividadDTO actividad) {
        try {
            ActividadSimpleDTO simple = new ActividadSimpleDTO();

            // Datos básicos (siempre disponibles)
            simple.setId(actividad.getId());
            simple.setTitulo(actividad.getTitulo());
            simple.setPrecio(actividad.getPrecio());

            // Crear CompletableFutures para llamadas paralelas
            CompletableFuture<String> imagenFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    var responseImagen = galeriaRestClient.getImagenPrincipal(actividad.getId());
                    if (responseImagen.getStatus() == 200) {
                        Map<String, Object> imagenData = responseImagen.readEntity(Map.class);
                        if (imagenData != null && imagenData.get("imagenBinaria") != null) {
                            return (String) imagenData.get("imagenBinaria");
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error obteniendo imagen para actividad " + actividad.getId() + ": " + e.getMessage());
                }
                return null;
            });

            CompletableFuture<Double> ratingFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    var responseRating = opinionRestClient.getPromedioPuntuacion(actividad.getId());
                    if (responseRating.getStatus() == 200) {
                        Map<String, Object> ratingData = responseRating.readEntity(Map.class);
                        Object promedio = ratingData.get("promedioPuntuacion");
                        if (promedio instanceof Number) {
                            return Math.round(((Number) promedio).doubleValue() * 100.0) / 100.0;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error obteniendo rating para actividad " + actividad.getId() + ": " + e.getMessage());
                }
                return 0.0;
            });

            // Esperar ambas operaciones con timeout
            try {
                // Timeout de 2 segundos para cada operación
                CompletableFuture.allOf(imagenFuture, ratingFuture)
                        .get(2, TimeUnit.SECONDS);

                simple.setImagen(imagenFuture.get());
                simple.setRating(ratingFuture.get());

            } catch (TimeoutException e) {
                System.err.println("Timeout procesando actividad " + actividad.getId());
                // Usar valores por defecto
                simple.setImagen(null);
                simple.setRating(0.0);
            }

            return simple;

        } catch (Exception e) {
            System.err.println("Error general procesando actividad " + actividad.getId() + ": " + e.getMessage());
            return null; // Se filtrará en el stream
        }
    }
}
