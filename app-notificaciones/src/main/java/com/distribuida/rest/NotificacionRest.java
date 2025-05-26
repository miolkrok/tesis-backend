package com.distribuida.rest;

import com.distribuida.db.Notificacion;
import com.distribuida.dtos.NotificacionDTO;
import com.distribuida.repo.NotificacionRepository;
import com.distribuida.service.NotificacionService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/notificaciones")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transactional
public class   NotificacionRest {

    @Inject
    private NotificacionRepository notificacionRepo;


    @Inject
    NotificacionService notificacionService;

    @GET
    public List<Notificacion> findAll() {
        return notificacionRepo.listAll();
    }

    @GET
    @Path("/{id}")
    public Response findById(@PathParam("id") Integer id) {
        var op = notificacionRepo.findByIdOptional(id);
        if (op.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(op.get()).build();
    }

    @GET
    @Path("/usuario/{usuarioId}")
    public List<Notificacion> findByUsuario(@PathParam("usuarioId") Integer usuarioId) {
        return notificacionRepo.find("usuarioId", usuarioId).list();
    }

    @GET
    @Path("/usuario/{usuarioId}/noLeidas")
    public List<Notificacion> findNoLeidasByUsuario(@PathParam("usuarioId") Integer usuarioId) {
        return notificacionRepo
                .find("usuarioId = ?1 AND estado != 'LEIDA'", usuarioId)
                .list();
    }

    @POST
    @Path("/marcarLeida/{id}")
    public Response marcarComoLeida(@PathParam("id") Integer id) {
        var op = notificacionRepo.findByIdOptional(id);
        if (op.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        Notificacion notificacion = op.get();
        notificacion.setEstado("LEIDA");
        notificacionRepo.persist(notificacion);

        return Response.ok().build();
    }

    @POST
    @Path("/enviar")
    public Response enviarNotificacion(NotificacionDTO evento) {
        try {
            NotificacionDTO resultado = notificacionService.crearEventoNotificacion(
                    evento.getUsuarioId(),
                    evento.getTitulo(),
                    evento.getMensaje(),
                    evento.getTipo(),
                    //evento.getRelacionadoId(),
                    evento.getTipoEntidad()
            );

            return Response.status(Response.Status.CREATED).entity(resultado).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al enviar notificación: " + e.getMessage())
                    .build();
        }
    }

    @POST
    @Path("/enviarReserva")
    public Response notificarReserva(@QueryParam("usuarioId") Integer usuarioId,
                                     @QueryParam("reservaId") Integer reservaId,
                                     @QueryParam("estado") String estadoReserva) {

        String titulo = "Actualización de reserva";
        String mensaje = "Tu reserva #" + reservaId + " ha cambiado su estado a: " + estadoReserva;

        try {
            NotificacionDTO resultado = notificacionService.crearEventoNotificacion(
                    usuarioId,
                    titulo,
                    mensaje,
                    "EMAIL",
                    reservaId,
                    "RESERVA"
            );

            return Response.status(Response.Status.CREATED).entity(resultado).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al enviar notificación: " + e.getMessage())
                    .build();
        }
    }

    @POST
    @Path("/enviarPago")
    public Response notificarPago(@QueryParam("usuarioId") Integer usuarioId,
                                  @QueryParam("pagoId") Integer pagoId,
                                  @QueryParam("estado") String estadoPago,
                                  @QueryParam("monto") String monto) {

        String titulo = "Actualización de pago";
        String mensaje = "Tu pago #" + pagoId + " por un monto de " + monto +
                " ha cambiado su estado a: " + estadoPago;

        try {
            NotificacionDTO resultado = notificacionService.crearEventoNotificacion(
                    usuarioId,
                    titulo,
                    mensaje,
                    "EMAIL",
                    pagoId,
                    "PAGO"
            );

            return Response.status(Response.Status.CREATED).entity(resultado).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al enviar notificación: " + e.getMessage())
                    .build();
        }
    }

    @POST
    @Path("/enviarActividad")
    public Response notificarActividad(@QueryParam("usuarioId") Integer usuarioId,
                                       @QueryParam("actividadId") Integer actividadId,
                                       @QueryParam("nombreActividad") String nombreActividad,
                                       @QueryParam("tipoEvento") String tipoEvento) {

        String titulo = "Novedad en actividad";
        String mensaje = "La actividad '" + nombreActividad + "' ha tenido un evento: " + tipoEvento;

        try {
            NotificacionDTO resultado = notificacionService.crearEventoNotificacion(
                    usuarioId,
                    titulo,
                    mensaje,
                    "EMAIL",
                    actividadId,
                    "ACTIVIDAD"
            );

            return Response.status(Response.Status.CREATED).entity(resultado).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al enviar notificación: " + e.getMessage())
                    .build();
        }
    }
}
