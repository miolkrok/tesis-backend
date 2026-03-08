package com.distribuida.clients;

import com.distribuida.dtos.ActividadDTO;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;

@Path("/actividades")
@RegisterRestClient(configKey = "actividad-api")
public interface ActividadRestClient {

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    ActividadDTO findById(@PathParam("id") Integer id);

    @GET
    @Path("/usuario/{usuarioId}")
    @Produces(MediaType.APPLICATION_JSON)
    List<ActividadDTO> findByUsuarioId(
            @HeaderParam("Authorization") String token,
            @PathParam("usuarioId") Integer usuarioId
    );
}