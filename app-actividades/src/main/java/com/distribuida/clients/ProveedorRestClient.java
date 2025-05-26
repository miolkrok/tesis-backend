package com.distribuida.clients;


import com.distribuida.dtos.ProveedorDto;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/proveedores")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
//@RegisterRestClient(baseUri = "http://localhost:9090")
@RegisterRestClient(configKey = "ProveedorRestClient")
//@RegisterRestClient(baseUri = "stork://my-service")
public interface  ProveedorRestClient {

    @GET
    @Path("/{id}")
    ProveedorDto findById(@PathParam("id") Integer id);
}
