package com.distribuida.rest;

import com.distribuida.clients.ReservaRestClient;
import com.distribuida.db.Pago;
import com.distribuida.dtos.PagoDTO;
import com.distribuida.repo.PagoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.time.LocalDateTime;
import java.util.List;

@Path("/pagos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transactional
public class PagoRest {

    @Inject
    private PagoRepository pagoRepo;

    @Inject
    @RestClient
    ReservaRestClient reservaRestClient;

    @GET
    public List<PagoDTO> findAll() {
        var pagos = pagoRepo.listAll();

        return pagos.stream()
                .map(pago -> {
                    try {
                        var reserva = reservaRestClient.findById(pago.getReservaId());

                        PagoDTO dto = new PagoDTO();
                        dto.setId(pago.getId());
                        dto.setReservaId(pago.getReservaId());
                        dto.setMonto(pago.getMonto());
                        dto.setFechaTransaccion(pago.getFechaTransaccion());
                        dto.setEstado(pago.getEstado());
                        dto.setMetodoPago(pago.getMetodoPago());
                        //dto.setReferencia(pago.getReferencia());

                        //Información enriquecida desde el servicio de reservas
                        //dto.setDetalleReserva(reserva.getNombreActividad() + " (" +
                                //reserva.getFechaInicio() + " - " + reserva.getFechaFin() + ")");
                        //dto.setNombreUsuario(reserva.getNombreUsuario());

                        return dto;
                    } catch (Exception e) {
                        PagoDTO dto = new PagoDTO();
                        dto.setId(pago.getId());
                        dto.setReservaId(pago.getReservaId());
                        dto.setMonto(pago.getMonto());
                        dto.setFechaTransaccion(pago.getFechaTransaccion());
                        dto.setEstado(pago.getEstado());
                        dto.setMetodoPago(pago.getMetodoPago());
                        //dto.setReferencia(transaccion.getReferencia());

                        return dto;
                    }
                }).toList();
    }

    @GET
    @Path("/{id}")
    public Response findById(@PathParam("id") Integer id) {
        var op = pagoRepo.findByIdOptional(id);
        if (op.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        Pago pago = op.get();
        try {
            var reserva = reservaRestClient.findById(pago.getReservaId());

            PagoDTO dto = new PagoDTO();
            dto.setId(pago.getId());
            dto.setReservaId(pago.getReservaId());
            dto.setMonto(pago.getMonto());
            dto.setFechaTransaccion(pago.getFechaTransaccion());
            dto.setEstado(pago.getEstado());
            dto.setMetodoPago(pago.getMetodoPago());
            //dto.setReferencia(pago.getReferencia());
            //dto.setDetalleReserva(reserva.getNombreActividad() + " (" +
              //      reserva.getFechaInicio() + " - " + reserva.getFechaFin() + ")");
            //dto.setNombreUsuario(reserva.getNombreUsuario());

            return Response.ok(dto).build();
        } catch (Exception e) {
            return Response.ok(pago).build();
        }
    }

    @POST
    public Response create(Pago pago) {
        // Validar que exista la reserva
        try {
            reservaRestClient.findById(pago.getReservaId());

            // Establecer fecha actual si no se proporcionó
            if (pago.getFechaTransaccion() == null) {
                pago.setFechaTransaccion(LocalDateTime.now());
            }

            pago.setId(null);
            pagoRepo.persist(pago);
            return Response.status(Response.Status.CREATED).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Error: Reserva no encontrada").build();
        }
    }
    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") Integer id, Pago pago) {
        Pago obj = pagoRepo.findById(id);

        obj.setReservaId(pago.getReservaId());
        obj.setMonto(pago.getMonto());
        obj.setFechaTransaccion(pago.getFechaTransaccion());
        obj.setEstado(pago.getEstado());
        obj.setMetodoPago(pago.getMetodoPago());
        //obj.setReferencia(pago.getReferencia());

        return Response.ok().build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Integer id) {
        pagoRepo.deleteById(id);
        return Response.ok().build();
    }

    @GET
    @Path("/reserva/{reservaId}")
    public List<PagoDTO> findByReserva(@PathParam("reservaId") Integer reservaId) {
        var transacciones = pagoRepo.find("reservaId", reservaId).list();

        return transacciones.stream()
                .map(transaccion -> {
                    try {
                        var reserva = reservaRestClient.findById(transaccion.getReservaId());

                        PagoDTO dto = new PagoDTO();
                        dto.setId(transaccion.getId());
                        dto.setReservaId(transaccion.getReservaId());
                        dto.setMonto(transaccion.getMonto());
                        dto.setFechaTransaccion(transaccion.getFechaTransaccion());
                        dto.setEstado(transaccion.getEstado());
                        dto.setMetodoPago(transaccion.getMetodoPago());
                        //dto.setReferencia(transaccion.getReferencia());
                        //dto.setDetalleReserva(reserva.getNombreActividad() + " (" +
                          //      reserva.getFechaInicio() + " - " + reserva.getFechaFin() + ")");
                        //dto.setNombreUsuario(reserva.getNombreUsuario());

                        return dto;
                    } catch (Exception e) {
                        PagoDTO dto = new PagoDTO();
                        dto.setId(transaccion.getId());
                        dto.setReservaId(transaccion.getReservaId());
                        dto.setMonto(transaccion.getMonto());
                        dto.setFechaTransaccion(transaccion.getFechaTransaccion());
                        dto.setEstado(transaccion.getEstado());
                        dto.setMetodoPago(transaccion.getMetodoPago());
                        //dto.setReferencia(transaccion.getReferencia());

                        return dto;
                    }
                }).toList();
    }
}
