package com.distribuida.clients;

import com.distribuida.dtos.ReservaDTO;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/reservas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
//@RegisterRestClient(baseUri = "http://localhost:9090")
@RegisterRestClient(configKey = "ReservaRestClient")
//@RegisterRestClient(baseUri = "stork://my-service")
public interface ReservaRestClient {

    @GET
    @Path("/{id}")
    ReservaDTO findById(@PathParam("id") Integer id);

}
