package com.distribuida.clients;

import com.distribuida.dtos.PagoDTO;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;

@Path("/pagos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RegisterRestClient(configKey = "PagoRestClient")
public interface PagoRestClient {

    @GET
    @Path("/reserva/{reservaId}")
    List<PagoDTO> findByReserva(@PathParam("reservaId") Integer reservaId);
}