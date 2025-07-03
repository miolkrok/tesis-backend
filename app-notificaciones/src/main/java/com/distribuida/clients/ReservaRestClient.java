package com.distribuida.clients;

import com.distribuida.config.RestClientConfig;
import com.distribuida.dtos.ReservaDTO;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;

@Path("/reservas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
//@RegisterRestClient(baseUri = "http://localhost:9090")
@RegisterRestClient(configKey = "ReservaRestClient")
//@RegisterRestClient(baseUri = "stork://my-service")
@RegisterClientHeaders(RestClientConfig.class)
public interface ReservaRestClient {

    @GET
    @Path("/{id}")
    ReservaDTO findById(@PathParam("id") Integer id);

    @GET
    List<ReservaDTO> findAll();

    @POST
    ReservaDTO create(ReservaDTO reserva);

    @GET
    @Path("/usuario/{usuarioId}")
    List<ReservaDTO> findByUsuario(@PathParam("usuarioId") Integer usuarioId);

}
