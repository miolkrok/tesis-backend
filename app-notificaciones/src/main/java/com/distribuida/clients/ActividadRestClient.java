package com.distribuida.clients;


import com.distribuida.dtos.ActividadDTO;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/actividades")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RegisterRestClient(configKey = "ActividadRestClient")
public interface ActividadRestClient {

    @GET
    @Path("/{id}")
    ActividadDTO findById(@PathParam("id") Integer id);

}
