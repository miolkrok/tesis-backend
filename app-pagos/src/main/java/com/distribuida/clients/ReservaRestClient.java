package com.distribuida.clients;

import com.distribuida.dtos.ReservaDTO;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/reservas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RegisterRestClient(baseUri = "stork://reservas-service")
public interface ReservaRestClient {

    @GET
    @Path("/{id}")
    ReservaDTO findById(@PathParam("id") Integer id);
}
