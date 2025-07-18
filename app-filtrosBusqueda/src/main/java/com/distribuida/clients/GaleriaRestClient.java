package com.distribuida.clients;

import com.distribuida.config.RestClientConfig;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/imagenes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RegisterRestClient(configKey = "GaleriaRestClient")
@RegisterClientHeaders(RestClientConfig.class)
public interface GaleriaRestClient {
    @GET
    @Path("/actividad/{actividadId}")
    Response getImagenesPorActividad(@PathParam("actividadId") Integer actividadId);

    @GET
    @Path("/actividad/{actividadId}/principal")
    Response getImagenPrincipal(@PathParam("actividadId") Integer actividadId);
}
