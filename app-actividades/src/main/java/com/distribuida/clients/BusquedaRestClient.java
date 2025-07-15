package com.distribuida.clients;

import com.distribuida.config.RestClientConfig;
import com.distribuida.dtos.BusquedaActividadDTO;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/busquedas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RegisterRestClient(configKey = "BusquedaRestClient")
@RegisterClientHeaders(RestClientConfig.class)
public interface BusquedaRestClient {

    /**
     * Indexar nueva actividad en el módulo de búsqueda
     */
    @POST
    @Path("/indexar")
    Response indexarActividad(BusquedaActividadDTO actividad);

    /**
     * Actualizar índice existente
     */
    @PUT
    @Path("/indexar/{actividadId}")
    Response actualizarIndice(@PathParam("actividadId") Integer actividadId,
                              BusquedaActividadDTO actividad);

    /**
     * Eliminar de índice de búsqueda
     */
    @DELETE
    @Path("/indexar/{actividadId}")
    Response eliminarDeBusqueda(@PathParam("actividadId") Integer actividadId);

    /**
     * Reindexar todas las actividades (para sincronización masiva)
     */
    @POST
    @Path("/reindexar")
    Response reindexarTodo();
}
