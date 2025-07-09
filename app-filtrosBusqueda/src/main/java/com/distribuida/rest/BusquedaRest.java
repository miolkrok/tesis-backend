package com.distribuida.rest;

import com.distribuida.clients.ActividadRestClient;
import com.distribuida.clients.OpinionRestClient;
import com.distribuida.db.Busqueda;
import com.distribuida.dtos.BusquedaActividadRequestSimple;
import com.distribuida.dtos.BusquedaActividadResponseSimple;
import com.distribuida.repo.BusquedaRepository;
import com.distribuida.service.CoordenadasService;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;



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
    OpinionRestClient opinionRestClient;

    @Inject
    CoordenadasService coordenadasService;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public BusquedaRest() {
        // Actualizar índices cada 30 minutos
        scheduler.scheduleAtFixedRate(this::actualizarIndices, 1, 30, TimeUnit.MINUTES);
    }

    /**
     * Búsqueda principal con funcionalidad mejorada
     */
    @POST
    @Path("/buscar")
    public Response buscarActividades(@Valid BusquedaActividadRequestSimple request) {
        try {
            System.out.println("Búsqueda de actividades: " + request);

            // Registrar búsqueda para analytics
            registrarBusqueda(request);

            List<Busqueda> resultados;

            // Si hay coordenadas, usar búsqueda por proximidad
            if (request.getLatitud() != null && request.getLongitud() != null) {
                resultados = buscarPorProximidad(request);
            } else if (request.getUbicacion() != null && !request.getUbicacion().trim().isEmpty()) {
                // Intentar geocodificar la ubicación
                CoordenadasService.Coordenadas coords = coordenadasService.obtenerCoordenadas(request.getUbicacion());
                resultados = buscarPorProximidadConGeocodificacion(request, coords);
            } else {
                // Búsqueda general
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
            System.err.println("Error en búsqueda: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "Error al realizar búsqueda", "detalle", e.getMessage()))
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
     * Búsqueda con filtros avanzados
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
     * Obtener actividades cercanas a una ubicación
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
     * Obtener filtros disponibles para búsqueda
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

    // ===== MÉTODOS PRIVADOS DE APOYO =====

    private void registrarBusqueda(BusquedaActividadRequestSimple request) {
        try {
            // Aquí podrías guardar el historial de búsquedas para analytics
            System.out.println("Registrando búsqueda: " + request.getUbicacion() +
                    " - Personas: " + request.getCantidadPersonas() +
                    " - Fechas: " + request.getFechaInicio() + " a " + request.getFechaFin());
        } catch (Exception e) {
            System.err.println("Error registrando búsqueda: " + e.getMessage());
        }
    }

    private List<Busqueda> buscarPorProximidad(BusquedaActividadRequestSimple request) {
        return busquedaRepository.buscarPorProximidad(
                request.getLatitud(),
                request.getLongitud(),
                request.getRadioKm() != null ? request.getRadioKm() : 50.0,
                20 // límite por defecto
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
            System.out.println("Iniciando actualización de índices de búsqueda...");

            var actividades = actividadRestClient.findAll();
            int indexadas = 0;
            int errores = 0;

            for (var actividad : actividades) {
                try {
                    Busqueda indice = busquedaRepository.find("actividadId", actividad.getId())
                            .firstResultOptional()
                            .orElse(new Busqueda());

                    // Mapear datos básicos
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

            System.out.println("Indexación completada: " + indexadas + " exitosas, " + errores + " errores");

        } catch (Exception e) {
            System.err.println("Error general en indexación: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ===== MÉTODOS HEREDADOS =====

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
                    "mensaje", "Indexación completada exitosamente",
                    "timestamp", System.currentTimeMillis()
            )).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                            "error", "Error durante la indexación",
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
                    .entity(Map.of("error", "Error al obtener estadísticas"))
                    .build();
        }
    }
}
