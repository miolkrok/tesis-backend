package com.distribuida.clients;

import com.distribuida.dtos.ActividadDTO;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;

@Path("/actividades")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RegisterRestClient(baseUri = "stork://actividades-service")
public interface ActividadRestClient {

    @GET
    List<ActividadDTO> findAll();

    @GET
    @Path("/{id}")
    ActividadDTO findById(@PathParam("id") Integer id);
}
