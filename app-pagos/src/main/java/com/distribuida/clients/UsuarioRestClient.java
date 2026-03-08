package com.distribuida.clients;

import com.distribuida.dtos.UsuarioDTO;
import com.distribuida.config.RestClientConfig;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;
import java.util.Map;

@Path("/usuarios")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RegisterRestClient(configKey = "UsuarioRestClient")
@RegisterClientHeaders(RestClientConfig.class)
public interface UsuarioRestClient {

    @GET
    @Path("/{id}")
    UsuarioDTO findById(@PathParam("id") Integer id);

    @GET
    List<UsuarioDTO> findAll();

    @GET
    @Path("/public/{id}")
    Map<String, Object> findByIdPublic(@PathParam("id") Integer id);
}
