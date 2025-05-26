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
        // Actualizar índices cada 15 minutos
        scheduler.scheduleAtFixedRate(this::actualizarIndices, 0, 15, TimeUnit.MINUTES);
    }

    @GET
    @Path("/indexar")
    public Response indexarActividades() {
        actualizarIndices();
        return Response.ok("Indexación iniciada").build();
    }

    @Transactional
    public void actualizarIndices() {
        try {
            // Obtener todas las actividades
            var actividades = actividadRestClient.findAll();

            for (var actividad : actividades) {
                try {
                    // Buscar valoraciones
                    var opinion = opinionRestClient.getPromedioPuntuacion(actividad.getId());

                    // Crear o actualizar índice
                    Busqueda indice = busquedaRepository.findByIdOptional(actividad.getId())
                            .orElse(new Busqueda());

                    indice.setActividadId(actividad.getId());
                    indice.setTitulo(actividad.getTitulo());
                    indice.setDescripcion(actividad.getDescripcion());
                    indice.setUbicacion(actividad.getUbicacion());
                    //indice.setCategoria(actividad.getCategoria());
                    indice.setPrecio(actividad.getPrecio());
                    //indice.setCapacidad(actividad.getCapacidad());
                    indice.setProveedorId(actividad.getProveedorId());
                    //indice.setNombreProveedor(actividad.getNombreProveedor());
                    //indice.setPuntuacionPromedio(resena.getPromedioPuntuacion());

                    busquedaRepository.persistAndFlush(indice);
                } catch (Exception e) {
                    // Si falla una actividad, continuar con las demás
                    System.err.println("Error indexando actividad ID: " + actividad.getId() + ": " + e.getMessage());
                }
            }

            System.out.println("Indexación completada. Actividades indexadas: " + actividades.size());
        } catch (Exception e) {
            System.err.println("Error en el proceso de indexación: " + e.getMessage());
        }
    }

    @GET
    public List<Busqueda> buscarGeneral(@QueryParam("q") String query) {
        if (query == null || query.isEmpty()) {
            return busquedaRepository.listAll();
        }
        return busquedaRepository.buscar(query);
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
    public List<Busqueda> buscarMejorValoradas() {
        return busquedaRepository.find("ORDER BY puntuacionPromedio DESC")
                .page(0, 10)
                .list();
    }
}
