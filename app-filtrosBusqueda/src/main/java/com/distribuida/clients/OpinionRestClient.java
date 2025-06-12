package com.distribuida.clients;

import com.distribuida.dtos.OpinionDTO;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/opiniones")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RegisterRestClient(configKey = "OpinionRestClient")
public interface OpinionRestClient {
    @GET
    @Path("/promedio/actividad/{actividadId}")
    OpinionDTO getPromedioPuntuacion(@PathParam("actividadId") Integer actividadId);
}
