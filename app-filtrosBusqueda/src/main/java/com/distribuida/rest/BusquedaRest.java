package com.distribuida.rest;

import com.distribuida.clients.ActividadRestClient;
import com.distribuida.clients.OpinionRestClient;
import com.distribuida.db.Busqueda;
import com.distribuida.repo.BusquedaRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.math.BigDecimal;
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

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    // Constructor que inicia el proceso de indexación periódica
    public BusquedaRest() {
        // Actualizar índices cada 30 minutos
        scheduler.scheduleAtFixedRate(this::actualizarIndices, 1, 30, TimeUnit.MINUTES);
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

    @Transactional
    public void actualizarIndices() {
        try {
            System.out.println("🔄 Iniciando proceso de indexación...");

            // Obtener todas las actividades
            var actividades = actividadRestClient.findAll();
            int indexadas = 0;
            int errores = 0;

            for (var actividad : actividades) {
                try {
                    // Buscar o crear índice para esta actividad
                    Busqueda indice = busquedaRepository.find("actividadId", actividad.getId())
                            .firstResultOptional()
                            .orElse(new Busqueda());

                    // Mapear datos básicos de la actividad
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

                    // Intentar obtener promedio de opiniones
                    try {
                        var responseOpinion = opinionRestClient.getPromedioPuntuacion(actividad.getId());
                        // Procesar respuesta de opiniones si es exitosa
                        if (responseOpinion != null) {
                            // Asumir que la respuesta contiene el promedio
                            indice.setPuntuacionPromedio(4.0); // Valor por defecto hasta implementar correctamente
                        }
                    } catch (Exception e) {
                        // Si falla, usar valor por defecto
                        indice.setPuntuacionPromedio(0.0);
                        System.out.println("⚠️ No se pudo obtener promedio de opiniones para actividad ID: " + actividad.getId());
                    }

                    // Guardar índice
                    busquedaRepository.persistAndFlush(indice);
                    indexadas++;

                } catch (Exception e) {
                    errores++;
                    System.err.println("❌ Error indexando actividad ID: " + actividad.getId() + ": " + e.getMessage());
                }
            }

            System.out.println("✅ Indexación completada:");
            System.out.println("   - Actividades indexadas: " + indexadas);
            System.out.println("   - Errores: " + errores);
            System.out.println("   - Total actividades: " + actividades.size());

        } catch (Exception e) {
            System.err.println("❌ Error general en el proceso de indexación: " + e.getMessage());
            e.printStackTrace();
        }
    }

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
    @Path("/filtros")
    public List<Busqueda> buscarConFiltros(
            @QueryParam("q") String query,
            @QueryParam("categoria") String categoria,
            @QueryParam("ubicacion") String ubicacion,
            @QueryParam("precioMin") BigDecimal precioMin,
            @QueryParam("precioMax") BigDecimal precioMax) {

        return busquedaRepository.buscarConFiltros(query, categoria, ubicacion, precioMin, precioMax);
    }

    @GET
    @Path("/mejorValoradas")
    public List<Busqueda> buscarMejorValoradas(@QueryParam("limite") @DefaultValue("10") int limite) {
        return busquedaRepository.find("ORDER BY puntuacionPromedio DESC, titulo ASC")
                .page(0, limite)
                .list();
    }

    @GET
    @Path("/populares")
    public List<Busqueda> buscarPopulares(@QueryParam("limite") @DefaultValue("10") int limite) {
        // Las más buscadas/con más reservas (simulado por ahora)
        return busquedaRepository.find("ORDER BY precio ASC, puntuacionPromedio DESC")
                .page(0, limite)
                .list();
    }

    @GET
    @Path("/recientes")
    public List<Busqueda> buscarRecientes(@QueryParam("limite") @DefaultValue("10") int limite) {
        return busquedaRepository.find("ORDER BY actividadId DESC")
                .page(0, limite)
                .list();
    }

    @GET
    @Path("/por-precio")
    public List<Busqueda> buscarPorRangoPrecio(
            @QueryParam("min") @DefaultValue("0") BigDecimal precioMin,
            @QueryParam("max") BigDecimal precioMax) {

        if (precioMax == null) {
            return busquedaRepository.find("precio >= ?1 ORDER BY precio ASC", precioMin).list();
        }
        return busquedaRepository.find("precio >= ?1 AND precio <= ?2 ORDER BY precio ASC", precioMin, precioMax).list();
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
