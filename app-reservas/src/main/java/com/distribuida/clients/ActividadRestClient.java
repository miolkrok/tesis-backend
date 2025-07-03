package com.distribuida.clients;

import com.distribuida.config.RestClientConfig;
import com.distribuida.dtos.ActividadDTO;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;

@Path("/actividades")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RegisterRestClient(configKey = "ActividadRestClient")
@RegisterClientHeaders(RestClientConfig.class)
public interface ActividadRestClient {
    @GET
    List<ActividadDTO> findAll();

    @GET
    @Path("/{id}")
    ActividadDTO findById(@PathParam("id") Integer id);

    @POST
    ActividadDTO create(ActividadDTO actividad);

    @PUT
    @Path("/{id}")
    ActividadDTO update(@PathParam("id") Integer id, ActividadDTO actividad);

    @DELETE
    @Path("/{id}")
    void delete(@PathParam("id") Integer id);
}
