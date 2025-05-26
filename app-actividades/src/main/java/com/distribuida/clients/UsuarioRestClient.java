package com.distribuida.clients;


import com.distribuida.dtos.UsuarioDTO;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/usuarios")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RegisterRestClient(baseUri = "stork://usuarios-service")

public interface UsuarioRestClient {
    @GET
    @Path("/{id}")
    UsuarioDTO findById(@PathParam("id") Integer id);
}
