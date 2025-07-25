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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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

    // Reemplazar el metodo busquedaRapida existente con este:
    @POST
    @Path("/busqueda-rapida-mejorada")
    @PermitAll
    public Response busquedaRapidaMejorada(@Valid BusquedaRapidaRequest request) {
        try {
            System.out.println("BUSQUEDA MEJORADA - Ubicacion: " + request.getUbicacion() +
                    " | Fechas: " + request.getFechaInicio() + " a " + request.getFechaFin() +
                    " | Personas: " + request.getCantidadPersonas());

            // === VALIDACIONES BASICAS ===
            if (!request.isValidDateRange()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Rango de fechas inválido - Fecha fin debe ser posterior a fecha inicio"))
                        .build();
            }

            if (!request.isValidPriceRange()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Rango de precios invalido"))
                        .build();
            }

            // === GEOCODIFICACION ===
            CoordenadasService.Coordenadas coordenadas = null;
            if (request.getLatitud() == null || request.getLongitud() == null) {
                coordenadas = coordenadasService.obtenerCoordenadas(request.getUbicacion());
                request.setLatitud(coordenadas.getLatitud());
                request.setLongitud(coordenadas.getLongitud());
                System.out.println("Coordenadas obtenidas: " + coordenadas.getLatitud() + ", " + coordenadas.getLongitud());
            }

            // === BÚSQUEDA PRINCIPAL CON FILTROS MEJORADOS ===
            List<Busqueda> resultadosBrutos = busquedaRepository.buscarConFiltrosAvanzados(
                    request.getUbicacion(),
                    request.getFechaInicio(),
                    request.getFechaFin(),
                    request.getCantidadPersonas(),
                    request.getTipoActividad(),
                    request.getPrecioMinimo() != null ? BigDecimal.valueOf(request.getPrecioMinimo()) : null,
                    request.getPrecioMaximo() != null ? BigDecimal.valueOf(request.getPrecioMaximo()) : null,
                    request.getLatitud(),
                    request.getLongitud(),
                    request.getRadioKm()
            );

            System.out.println("Resultados brutos encontrados: " + resultadosBrutos.size());

            // === FILTRADO ESTRICTO POR DISPONIBILIDAD ===
            List<Busqueda> resultadosFiltrados = resultadosBrutos.stream()
                    .filter(actividad -> verificarDisponibilidadCompleta(actividad, request))
                    .collect(Collectors.toList());

            System.out.println("Resultados despues de filtrado: " + resultadosFiltrados.size());

            // === ORDENAMIENTO ===
            resultadosFiltrados = aplicarOrdenamiento(resultadosFiltrados, request);

            // === CONVERTIR A RESULTADOS CON LOS CAMPOS ESPECIFICOS QUE NECESITAS ===
            List<ActividadBusquedaSimpleDTO> resultadosMejorados = convertirAResultadosSimples(
                    resultadosFiltrados, request);

            System.out.println("Resultados con datos completos: " + resultadosMejorados.size());

            // === APLICAR PAGINACION ===
            int inicio = request.getPagina() * request.getTamanoPagina();
            int fin = Math.min(inicio + request.getTamanoPagina(), resultadosMejorados.size());

            List<ActividadBusquedaSimpleDTO> resultadosPaginados = resultadosMejorados.subList(
                    Math.min(inicio, resultadosMejorados.size()), fin);

            // === CREAR RESPUESTA SIMPLIFICADA ===
            Map<String, Object> response = new HashMap<>();
            response.put("actividades", resultadosPaginados);
            response.put("totalElementos", resultadosMejorados.size());
            response.put("paginaActual", request.getPagina());
            response.put("elementosPorPagina", request.getTamanoPagina());
            response.put("totalPaginas", (int) Math.ceil((double) resultadosMejorados.size() / request.getTamanoPagina()));
            response.put("hayMasPaginas", fin < resultadosMejorados.size());
            response.put("ubicacionBuscada", request.getUbicacion());
            response.put("cantidadPersonas", request.getCantidadPersonas());
            response.put("diasActividad", request.getDiasActividad());

            // Coordenadas
            Map<String, Object> coordenada = new HashMap<>();
            coordenada.put("latitud", request.getLatitud());
            coordenada.put("longitud", request.getLongitud());
            response.put("coordenadas", coordenada);

            // Resumen de búsqueda
            Map<String, Object> resumenBusqueda = new HashMap<>();
            resumenBusqueda.put("resultadosBrutos", resultadosBrutos.size());
            resumenBusqueda.put("resultadosFiltrados", resultadosFiltrados.size());
            resumenBusqueda.put("resultadosFinales", resultadosPaginados.size());
            response.put("resumenBusqueda", resumenBusqueda);

            System.out.println("Búsqueda completada exitosamente");
            return Response.ok(response).build();

        } catch (Exception e) {
            System.err.println("Error en busqueda rapida mejorada: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                            "error", "Error al realizar la busqueda",
                            "message", e.getMessage(),
                            "timestamp", LocalDateTime.now().toString()
                    ))
                    .build();
        }
    }

    private List<ActividadBusquedaSimpleDTO> convertirAResultadosSimples(
            List<Busqueda> actividades, BusquedaRapidaRequest request) {

        return actividades.stream()
                .map(busqueda -> {
                    ActividadBusquedaSimpleDTO resultado = new ActividadBusquedaSimpleDTO();

                    try {
                        // 1. OBTENER DATOS BASICOS DE LA ACTIVIDAD DESDE EL MODULO DE ACTIVIDADES
                        ActividadDTO actividad = actividadRestClient.findById(busqueda.getActividadId());

                        if (actividad == null) {
                            System.err.println("Actividad no encontrada: " + busqueda.getActividadId());
                            return null;
                        }

                        // CAMPOS BASICOS
                        resultado.setId(actividad.getId());
                        resultado.setTitulo(actividad.getTitulo());
                        resultado.setPrecio(actividad.getPrecio());

                        // 2. OBTENER IMAGEN PRINCIPAL DESDE EL MÓDULO DE ACTIVIDADES
                        try {
                            var responseImagenPrincipal = galeriaRestClient.getImagenPrincipal(actividad.getId());
                            if (responseImagenPrincipal.getStatus() == 200) {
                                Map<String, Object> imagenData = (Map<String, Object>) responseImagenPrincipal.readEntity(Map.class);
                                if (imagenData != null && imagenData.get("imagenBinaria") != null) {
                                    resultado.setImagen((String) imagenData.get("imagenBinaria"));
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("Error obteniendo imagen para actividad " + actividad.getId() + ": " + e.getMessage());
                            resultado.setImagen(null);
                        }

                        // 3. OBTENER RATING PROMEDIO DESDE EL MÓDULO DE OPINIONES
                        try {
                            var responseOpinion = opinionRestClient.getPromedioPuntuacion(actividad.getId());
                            if (responseOpinion.getStatus() == 200) {
                                Map<String, Object> opinionData = (Map<String, Object>) responseOpinion.readEntity(Map.class);
                                Object promedio = opinionData.get("promedioPuntuacion");
                                Object total = opinionData.get("totalOpiniones");

                                if (promedio instanceof Number) {
                                    double rating = ((Number) promedio).doubleValue();
                                    resultado.setRating(Math.round(rating * 100.0) / 100.0);
                                } else {
                                    resultado.setRating(0.0);
                                }

                                if (total instanceof Number) {
                                    resultado.setTotalOpiniones(((Number) total).intValue());
                                } else {
                                    resultado.setTotalOpiniones(0);
                                }
                            } else {
                                resultado.setRating(0.0);
                                resultado.setTotalOpiniones(0);
                            }
                        } catch (Exception e) {
                            System.err.println("Error obteniendo rating para actividad " + actividad.getId() + ": " + e.getMessage());
                            resultado.setRating(0.0);
                            resultado.setTotalOpiniones(0);
                        }

                        // 4. CALCULAR DISTANCIA SI HAY COORDENADAS
                        if (request.getLatitud() != null && request.getLongitud() != null &&
                                busqueda.getLatitud() != null && busqueda.getLongitud() != null) {
                            double distancia = coordenadasService.calcularDistancia(
                                    request.getLatitud(), request.getLongitud(),
                                    busqueda.getLatitud(), busqueda.getLongitud()
                            );
                            resultado.setDistanciaKm(Math.round(distancia * 100.0) / 100.0);
                        }

                        System.out.println("Procesada actividad: " + actividad.getId() + " - " + actividad.getTitulo() +
                                " | Rating: " + resultado.getRating() +
                                " | Imagen: " + (resultado.getImagen() != null ? "SI" : "NO"));

                        return resultado;

                    } catch (Exception e) {
                        System.err.println("Error procesando actividad " + busqueda.getActividadId() + ": " + e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull) // Filtrar resultados nulos
                .collect(Collectors.toList());
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

    @POST
    @Path("/busqueda-rapida")
    @PermitAll
    public Response busquedaRapida(@Valid BusquedaRapidaRequest request) {
        try {
            System.out.println("Busqueda Ubicacion: " + request.getUbicacion() +
                    " | Fechas: " + request.getFechaInicio() + " a " + request.getFechaFin() +
                    " | Personas: " + request.getCantidadPersonas());

            // === VALIDACIONES ===
            if (!request.isValidDateRange()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Rango de fechas invalido"))
                        .build();
            }

            if (!request.isValidPriceRange()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "Rango de precios invalido"))
                        .build();
            }

            // GEOCODIFICACION
            CoordenadasService.Coordenadas coordenadas = null;
            if (request.getLatitud() == null || request.getLongitud() == null) {
                coordenadas = coordenadasService.obtenerCoordenadas(request.getUbicacion());
                request.setLatitud(coordenadas.getLatitud());
                request.setLongitud(coordenadas.getLongitud());
            }

            // BUSQUEDA PRINCIPAL
            List<Busqueda> resultadosBrutos = busquedaRepository.buscarConFiltrosAvanzados(
                    request.getUbicacion(),
                    request.getFechaInicio(),
                    request.getFechaFin(),
                    request.getCantidadPersonas(),
                    request.getTipoActividad(),
                    request.getPrecioMinimo() != null ? BigDecimal.valueOf(request.getPrecioMinimo()) : null,
                    request.getPrecioMaximo() != null ? BigDecimal.valueOf(request.getPrecioMaximo()) : null,
                    request.getLatitud(),
                    request.getLongitud(),
                    request.getRadioKm()
            );

            // FILTRAR POR DISPONIBILIDAD ESTRICTA
            List<Busqueda> resultadosFiltrados = resultadosBrutos.stream()
                    .filter(actividad -> verificarDisponibilidadCompleta(actividad, request))
                    .collect(Collectors.toList());

            // ORDENAMIENTO
            resultadosFiltrados = aplicarOrdenamiento(resultadosFiltrados, request);

            // CREAR RESPUESTA PAGINADA
            BusquedaRapidaResponse response = crearRespuestaBusqueda(
                    resultadosFiltrados,
                    request,
                    coordenadas
            );

            System.out.println("Busqueda completada: " + response.getActividadesEncontradas() +
                    " actividades encontradas");

            return Response.ok(response).build();

        } catch (Exception e) {
            System.err.println("Error en busqueda rapida: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of(
                            "error", "Error al realizar la busqueda",
                            "message", e.getMessage()
                    ))
                    .build();
        }
    }

//  METODOS AUXILIARES

    private boolean verificarDisponibilidadCompleta(Busqueda actividad, BusquedaRapidaRequest request) {
        // 1. VERIFICAR ESTADO ACTIVO
        if (!"ACTIVA".equals(actividad.getEstadoActividad())) {
            return false;
        }

        // 2. VERIFICAR UBICACIoN (Ya filtrado en la consulta principal, pero doble check)
        if (request.getUbicacion() != null && !request.getUbicacion().trim().isEmpty()) {
            String ubicacionBusqueda = request.getUbicacion().toLowerCase();
            boolean ubicacionValida =
                    (actividad.getUbicacion() != null && actividad.getUbicacion().toLowerCase().contains(ubicacionBusqueda)) ||
                            (actividad.getCiudad() != null && actividad.getCiudad().toLowerCase().contains(ubicacionBusqueda)) ||
                            (actividad.getProvincia() != null && actividad.getProvincia().toLowerCase().contains(ubicacionBusqueda));

            if (!ubicacionValida) {
                return false;
            }
        }

        // 3. VERIFICAR FECHAS DE DISPONIBILIDAD
        if (request.getFechaInicio() != null && request.getFechaFin() != null) {
            // La actividad debe estar disponible en todo el rango solicitado
            if (actividad.getFechaInicioDisponible() != null &&
                    request.getFechaInicio().isBefore(actividad.getFechaInicioDisponible())) {
                return false;
            }

            if (actividad.getFechaFinDisponible() != null &&
                    request.getFechaFin().isAfter(actividad.getFechaFinDisponible())) {
                return false;
            }
        }

        // 4. VERIFICAR CAPACIDAD DE PERSONAS
        if (request.getCantidadPersonas() != null) {
            // Verificar minimo de personas
            if (actividad.getMinimoPersonas() != null &&
                    request.getCantidadPersonas() < actividad.getMinimoPersonas()) {
                return false;
            }

            // Verificar maximo de personas
            if (actividad.getMaximoPersonas() != null &&
                    request.getCantidadPersonas() > actividad.getMaximoPersonas()) {
                return false;
            }
        }

        return true;
    }

    private List<Busqueda> aplicarOrdenamiento(List<Busqueda> resultados, BusquedaRapidaRequest request) {
        return switch (request.getOrdenarPor()) {
            case "PRECIO_ASC" -> resultados.stream()
                    .sorted(Comparator.comparing(Busqueda::getPrecio,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .collect(Collectors.toList());

            case "PRECIO_DESC" -> resultados.stream()
                    .sorted(Comparator.comparing(Busqueda::getPrecio,
                            Comparator.nullsFirst(Comparator.reverseOrder())))
                    .collect(Collectors.toList());

            case "DISTANCIA" -> resultados.stream()
                    .filter(r -> r.getLatitud() != null && r.getLongitud() != null)
                    .sorted((a, b) -> {
                        double distA = coordenadasService.calcularDistancia(
                                request.getLatitud(), request.getLongitud(),
                                a.getLatitud(), a.getLongitud());
                        double distB = coordenadasService.calcularDistancia(
                                request.getLatitud(), request.getLongitud(),
                                b.getLatitud(), b.getLongitud());
                        return Double.compare(distA, distB);
                    })
                    .collect(Collectors.toList());

            default -> resultados.stream() // RELEVANCIA
                    .sorted(Comparator
                            .comparing(Busqueda::getPuntuacionPromedio,
                                    Comparator.nullsLast(Comparator.reverseOrder()))
                            .thenComparing(Busqueda::getNumeroReservas,
                                    Comparator.nullsLast(Comparator.reverseOrder())))
                    .collect(Collectors.toList());
        };
    }

    private BusquedaRapidaResponse crearRespuestaBusqueda(
            List<Busqueda> resultados,
            BusquedaRapidaRequest request,
            CoordenadasService.Coordenadas coordenadas) {

        BusquedaRapidaResponse response = new BusquedaRapidaResponse();

        // PAGINACION
        int inicio = request.getPagina() * request.getTamanoPagina();
        int fin = Math.min(inicio + request.getTamanoPagina(), resultados.size());

        List<Busqueda> resultadosPaginados = resultados.subList(
                Math.min(inicio, resultados.size()), fin);

        // CONVERTIR A DTO
        List<ActividadBusquedaDTO> actividadesDTO = resultadosPaginados.stream()
                .map(actividad -> convertirAActividadBusquedaDTO(actividad, request))
                .collect(Collectors.toList());

        response.setActividades(actividadesDTO);
        response.setTotalElementos(resultados.size());
        response.setPaginaActual(request.getPagina());
        response.setElementosPorPagina(request.getTamanoPagina());
        response.setTotalPaginas((int) Math.ceil((double) resultados.size() / request.getTamanoPagina()));
        response.setHayMasPaginas(fin < resultados.size());

        // METADATOS
        response.setUbicacionBuscada(request.getUbicacion());
        response.setFechaInicio(request.getFechaInicio().toString());
        response.setFechaFin(request.getFechaFin().toString());
        response.setCantidadPersonas(request.getCantidadPersonas());
        response.setDiasActividad(request.getDiasActividad());
        response.setActividadesEncontradas(resultados.size());

        // === ESTADISTICAS ===
        if (!resultados.isEmpty()) {
            double precioPromedio = resultados.stream()
                    .filter(r -> r.getPrecio() != null)
                    .mapToDouble(r -> r.getPrecio().doubleValue())
                    .average()
                    .orElse(0.0);
            response.setPrecioPromedio(Math.round(precioPromedio * 100.0) / 100.0);

            // Calcular distancia promedio
            if (request.getLatitud() != null && request.getLongitud() != null) {
                double distanciaPromedio = resultados.stream()
                        .filter(r -> r.getLatitud() != null && r.getLongitud() != null)
                        .mapToDouble(r -> coordenadasService.calcularDistancia(
                                request.getLatitud(), request.getLongitud(),
                                r.getLatitud(), r.getLongitud()))
                        .average()
                        .orElse(0.0);
                response.setDistanciaPromedio(Math.round(distanciaPromedio * 100.0) / 100.0);
            }
        }

        // === FILTROS DISPONIBLES ===
        response.setTiposActividadDisponibles(
                resultados.stream()
                        .map(Busqueda::getTipoActividad)
                        .filter(tipo -> tipo != null && !tipo.isEmpty())
                        .distinct()
                        .collect(Collectors.toList())
        );

        response.setProvinciasCercanas(
                resultados.stream()
                        .map(Busqueda::getProvincia)
                        .filter(prov -> prov != null && !prov.isEmpty())
                        .distinct()
                        .limit(5)
                        .collect(Collectors.toList())
        );

        // === RANGOS DE PRECIOS ===
        if (!resultados.isEmpty()) {
            List<BigDecimal> precios = resultados.stream()
                    .map(Busqueda::getPrecio)
                    .filter(precio -> precio != null)
                    .collect(Collectors.toList());

            if (!precios.isEmpty()) {
                response.setRangosPrecios(Map.of(
                        "minimo", precios.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO),
                        "maximo", precios.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO),
                        "promedio", response.getPrecioPromedio()
                ));
            }
        }

        // === SUGERENCIAS ===
        response.setSugerenciasUbicacion(
                coordenadasService.obtenerSugerenciasUbicacion(request.getUbicacion())
                        .stream()
                        .map(CoordenadasService.UbicacionSugerencia::getNombre)
                        .limit(3)
                        .collect(Collectors.toList())
        );

        return response;
    }

    private List<BusquedaRapidaResultDTO> convertirAResultadosMejoradosParalelo(
            List<Busqueda> actividades, BusquedaRapidaRequest request) {

        return actividades.parallelStream()
                .map(actividad -> {
                    BusquedaRapidaResultDTO resultado = new BusquedaRapidaResultDTO();

                    // INFORMACIoN BaSICA DE LA ACTIVIDAD
                    resultado.setId(actividad.getActividadId());
                    resultado.setTitulo(actividad.getTitulo());
                    resultado.setPrecio(actividad.getPrecio());
                    resultado.setUbicacionDestino(actividad.getUbicacion());
                    resultado.setTipoActividad(actividad.getTipoActividad());
                    resultado.setDuracion(actividad.getDuracion());
                    resultado.setMinimoPersonas(actividad.getMinimoPersonas());
                    resultado.setMaximoPersonas(actividad.getMaximoPersonas());

                    // CALCULAR DISTANCIA SI HAY COORDENADAS
                    if (request.getLatitud() != null && request.getLongitud() != null &&
                            actividad.getLatitud() != null && actividad.getLongitud() != null) {
                        double distancia = coordenadasService.calcularDistancia(
                                request.getLatitud(), request.getLongitud(),
                                actividad.getLatitud(), actividad.getLongitud()
                        );
                        resultado.setDistanciaKm(Math.round(distancia * 100.0) / 100.0);
                    }

                    // OBTENER RATING PROMEDIO DE OPINIONES
                    try {
                        var responseOpinion = opinionRestClient.getPromedioPuntuacion(actividad.getActividadId());
                        if (responseOpinion.getStatus() == 200) {
                            Map<String, Object> opinionData = (Map<String, Object>) responseOpinion.readEntity(Map.class);

                            Object promedio = opinionData.get("promedioPuntuacion");
                            Object total = opinionData.get("totalOpiniones");

                            if (promedio instanceof Number) {
                                double rating = ((Number) promedio).doubleValue();
                                resultado.setRating(Math.round(rating * 100.0) / 100.0);
                            } else {
                                resultado.setRating(0.0);
                            }

                            if (total instanceof Number) {
                                resultado.setTotalOpiniones(((Number) total).intValue());
                            } else {
                                resultado.setTotalOpiniones(0);
                            }
                        } else {
                            resultado.setRating(0.0);
                            resultado.setTotalOpiniones(0);
                        }
                    } catch (Exception e) {
                        System.err.println("Error obteniendo rating para actividad " +
                                actividad.getActividadId() + ": " + e.getMessage());
                        resultado.setRating(0.0);
                        resultado.setTotalOpiniones(0);
                    }

                    // OBTENER IMaGENES DE LA ACTIVIDAD
                    try {
                        // Obtener imagen principal
                        var responseImagenPrincipal = galeriaRestClient.getImagenPrincipal(actividad.getActividadId());
                        if (responseImagenPrincipal.getStatus() == 200) {
                            Map<String, Object> imagenPrincipalData = (Map<String, Object>) responseImagenPrincipal.readEntity(Map.class);
                            if (imagenPrincipalData != null) {
                                BusquedaRapidaResultDTO.ImagenActividadDTO imgDTO =
                                        new BusquedaRapidaResultDTO.ImagenActividadDTO();
                                imgDTO.setId((Integer) imagenPrincipalData.get("id"));
                                imgDTO.setImagenBase64((String) imagenPrincipalData.get("imagenBinaria"));
                                imgDTO.setNombreArchivo((String) imagenPrincipalData.get("nombreArchivo"));
                                imgDTO.setTipoContenido((String) imagenPrincipalData.get("tipoContenido"));
                                imgDTO.setEsImagenPrincipal(true);
                                resultado.setImagenPrincipal(imgDTO);
                            }
                        }

                        // Obtener todas las imagenes (opcional, para galeria completa)
                        var responseImagenes = galeriaRestClient.getImagenesPorActividad(actividad.getActividadId());
                        if (responseImagenes.getStatus() == 200) {
                            List<Map<String, Object>> imagenesData = (List<Map<String, Object>>) responseImagenes.readEntity(List.class);
                            if (imagenesData != null && !imagenesData.isEmpty()) {
                                List<BusquedaRapidaResultDTO.ImagenActividadDTO> imagenesDTO = imagenesData.stream()
                                        .map(imgMap -> {
                                            BusquedaRapidaResultDTO.ImagenActividadDTO imgDTO =
                                                    new BusquedaRapidaResultDTO.ImagenActividadDTO();
                                            imgDTO.setId((Integer) imgMap.get("id"));
                                            imgDTO.setImagenBase64((String) imgMap.get("imagenBinaria"));
                                            imgDTO.setNombreArchivo((String) imgMap.get("nombreArchivo"));
                                            imgDTO.setTipoContenido((String) imgMap.get("tipoContenido"));
                                            imgDTO.setEsImagenPrincipal((Boolean) imgMap.get("esImagenPrincipal"));
                                            return imgDTO;
                                        })
                                        .collect(Collectors.toList());
                                resultado.setImagenes(imagenesDTO);
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Error obteniendo imagenes para actividad " +
                                actividad.getActividadId() + ": " + e.getMessage());
                        resultado.setImagenes(List.of());
                    }

                    return resultado;
                })
                .collect(Collectors.toList());
    }

    private List<BusquedaRapidaResultDTO> convertirAResultadosMejorados(
            List<Busqueda> actividades, BusquedaRapidaRequest request) {

        return actividades.parallelStream()
                .map(actividad -> {
                    BusquedaRapidaResultDTO resultado = new BusquedaRapidaResultDTO();

                    // Informacion basica
                    resultado.setId(actividad.getActividadId());
                    resultado.setTitulo(actividad.getTitulo());
                    resultado.setPrecio(actividad.getPrecio());
                    resultado.setUbicacionDestino(actividad.getUbicacion());
                    resultado.setTipoActividad(actividad.getTipoActividad());
                    resultado.setDuracion(actividad.getDuracion());
                    resultado.setMinimoPersonas(actividad.getMinimoPersonas());
                    resultado.setMaximoPersonas(actividad.getMaximoPersonas());

                    // Calcular distancia si hay coordenadas
                    if (request.getLatitud() != null && request.getLongitud() != null &&
                            actividad.getLatitud() != null && actividad.getLongitud() != null) {
                        double distancia = coordenadasService.calcularDistancia(
                                request.getLatitud(), request.getLongitud(),
                                actividad.getLatitud(), actividad.getLongitud()
                        );
                        resultado.setDistanciaKm(Math.round(distancia * 100.0) / 100.0);
                    }

                    // Obtener rating promedio de opiniones
                    try {
                        var responseOpinion = opinionRestClient.getPromedioPuntuacion(actividad.getActividadId());
                        if (responseOpinion.getStatus() == 200) {
                            Map<String, Object> opinionData = responseOpinion.readEntity(Map.class);
                            Object promedio = opinionData.get("promedioPuntuacion");
                            Object total = opinionData.get("totalOpiniones");

                            if (promedio instanceof Number) {
                                resultado.setRating(((Number) promedio).doubleValue());
                            }
                            if (total instanceof Number) {
                                resultado.setTotalOpiniones(((Number) total).intValue());
                            }
                        } else {
                            resultado.setRating(0.0);
                            resultado.setTotalOpiniones(0);
                        }
                    } catch (Exception e) {
                        System.err.println("Error al obtener rating para actividad " +
                                actividad.getActividadId() + ": " + e.getMessage());
                        resultado.setRating(0.0);
                        resultado.setTotalOpiniones(0);
                    }

                    // Obtener imagenes de la actividad
                    try {
                        // Obtener imagen principal
                        var responseImagenPrincipal = galeriaRestClient.getImagenPrincipal(actividad.getActividadId());
                        if (responseImagenPrincipal.getStatus() == 200) {
                            GaleriaDTO imagenPrincipal = responseImagenPrincipal.readEntity(GaleriaDTO.class);
                            if (imagenPrincipal != null) {
                                BusquedaRapidaResultDTO.ImagenActividadDTO imgDTO =
                                        new BusquedaRapidaResultDTO.ImagenActividadDTO();
                                imgDTO.setId(imagenPrincipal.getId());
                                imgDTO.setImagenBase64(imagenPrincipal.getImagenBinaria());
                                imgDTO.setNombreArchivo(imagenPrincipal.getNombreArchivo());
                                imgDTO.setTipoContenido(imagenPrincipal.getTipoContenido());
                                imgDTO.setEsImagenPrincipal(true);
                                resultado.setImagenPrincipal(imgDTO);
                            }
                        }

                        // Obtener todas las imagenes
                        var responseImagenes = galeriaRestClient.getImagenesPorActividad(actividad.getActividadId());
                        if (responseImagenes.getStatus() == 200) {
                            List<GaleriaDTO> imagenes = responseImagenes.readEntity(List.class);
                            if (imagenes != null && !imagenes.isEmpty()) {
                                List<BusquedaRapidaResultDTO.ImagenActividadDTO> imagenesDTO = imagenes.stream()
                                        .map(img -> {
                                            BusquedaRapidaResultDTO.ImagenActividadDTO imgDTO =
                                                    new BusquedaRapidaResultDTO.ImagenActividadDTO();
                                            if (img instanceof Map) {
                                                Map<String, Object> imgMap = (Map<String, Object>) img;
                                                imgDTO.setId((Integer) imgMap.get("id"));
                                                imgDTO.setImagenBase64((String) imgMap.get("imagenBase64"));
                                                imgDTO.setNombreArchivo((String) imgMap.get("nombreArchivo"));
                                                imgDTO.setTipoContenido((String) imgMap.get("tipoContenido"));
                                                imgDTO.setEsImagenPrincipal((Boolean) imgMap.get("esImagenPrincipal"));
                                            }
                                            return imgDTO;
                                        })
                                        .collect(Collectors.toList());
                                resultado.setImagenes(imagenesDTO);
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Error al obtener imagenes para actividad " +
                                actividad.getActividadId() + ": " + e.getMessage());
                        resultado.setImagenes(List.of());
                    }

                    return resultado;
                })
                .collect(Collectors.toList());
    }

    private ActividadBusquedaDTO convertirAActividadBusquedaDTO(Busqueda actividad, BusquedaRapidaRequest request) {
        ActividadBusquedaDTO dto = new ActividadBusquedaDTO();

        // Informacion basica
        dto.setId(actividad.getActividadId());
        dto.setTitulo(actividad.getTitulo());
        dto.setDescripcion(actividad.getDescripcion());

        // Ubicacion
        dto.setUbicacionDestino(actividad.getUbicacion());
        dto.setProvincia(actividad.getProvincia());
        dto.setCiudad(actividad.getCiudad());
        dto.setLatitud(actividad.getLatitud());
        dto.setLongitud(actividad.getLongitud());

        // Calcular distancia si tenemos coordenadas
        if (request.getLatitud() != null && request.getLongitud() != null &&
                actividad.getLatitud() != null && actividad.getLongitud() != null) {
            double distancia = coordenadasService.calcularDistancia(
                    request.getLatitud(), request.getLongitud(),
                    actividad.getLatitud(), actividad.getLongitud()
            );
            dto.setDistanciaKm(Math.round(distancia * 100.0) / 100.0);
        }

        // Precio
        dto.setPrecio(actividad.getPrecio() != null ? actividad.getPrecio().doubleValue() : null);
        if (dto.getPrecio() != null) {
            dto.setPrecioTotal(dto.getPrecio() * request.getCantidadPersonas() * request.getDiasActividad());
        }

        // Disponibilidad
        dto.setDisponible(verificarDisponibilidadCompleta(actividad, request));
        if (!dto.getDisponible()) {
            dto.setMotivoNoDisponible(determinarMotivoNoDisponible(actividad, request));
        }

        // Caracteristicas
        dto.setTipoActividad(actividad.getTipoActividad());
        dto.setNivelDificultad(actividad.getNivelDificultad());
        dto.setDuracion(actividad.getDuracion());
        dto.setMinimoPersonas(actividad.getMinimoPersonas());
        dto.setMaximoPersonas(actividad.getMaximoPersonas());

        // Calificaciones
        dto.setPuntuacionPromedio(actividad.getPuntuacionPromedio());
        dto.setNumeroResenias(actividad.getNumeroOpiniones());
        dto.setNumeroReservas(actividad.getNumeroReservas());

        // Proveedor
        dto.setNombreProveedor(actividad.getNombreProveedor());

        // Etiquetas utiles
        List<String> etiquetas = new ArrayList<>();
        if (actividad.getNumeroReservas() != null && actividad.getNumeroReservas() > 10) {
            etiquetas.add("Popular");
        }
        if (actividad.getPuntuacionPromedio() != null && actividad.getPuntuacionPromedio() >= 4.5) {
            etiquetas.add("Excelente valoracion");
        }
        if (request.getCantidadPersonas() >= (actividad.getMinimoPersonas() != null ? actividad.getMinimoPersonas() : 1)) {
            etiquetas.add("Perfecto para grupos");
        }
        dto.setEtiquetas(etiquetas);

        // Caracteristicas adicionales
        dto.setCancelacionGratuita(true); // Por defecto, puedes implementar logica especifica
        dto.setConfirmacionInmediata(true);

        return dto;
    }

    private String determinarMotivoNoDisponible(Busqueda actividad, BusquedaRapidaRequest request) {
        if (actividad.getFechaInicioDisponible() != null &&
                request.getFechaInicio().isBefore(actividad.getFechaInicioDisponible())) {
            return "No disponible para las fechas seleccionadas";
        }

        if (actividad.getMinimoPersonas() != null &&
                request.getCantidadPersonas() < actividad.getMinimoPersonas()) {
            return "Requiere minimo " + actividad.getMinimoPersonas() + " personas";
        }

        if (actividad.getMaximoPersonas() != null &&
                request.getCantidadPersonas() > actividad.getMaximoPersonas()) {
            return "Excede capacidad maxima de " + actividad.getMaximoPersonas() + " personas";
        }

        return "No disponible";
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
}
