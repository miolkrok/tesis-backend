package com.distribuida.service;

import com.distribuida.clients.UsuarioRestClient;
import com.distribuida.db.Notificacion;
import com.distribuida.dtos.NotificacionDTO;
import com.distribuida.dtos.UsuarioDTO;
import com.distribuida.repo.NotificacionRepository;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.time.LocalDateTime;

@ApplicationScoped
public class NotificacionService {

    @Inject
    NotificacionRepository notificacionRepository;

    @Inject
    @RestClient
    UsuarioRestClient usuarioRestClient;

    @Inject
    Mailer mailer;

    @Inject
    @Channel("notificaciones-out")
    Emitter<NotificacionDTO> notificacionEmitter;

    @Incoming("eventos-reserva")
    @Transactional
    public void procesarEventoReserva(NotificacionDTO evento) {
        crearNotificacion(evento);
    }

    @Incoming("eventos-pago")
    @Transactional
    public void procesarEventoPago(NotificacionDTO evento) {
        crearNotificacion(evento);
    }

    @Incoming("eventos-actividad")
    @Transactional
    public void procesarEventoActividad(NotificacionDTO evento) {
        crearNotificacion(evento);
    }

    @Transactional
    public void crearNotificacion(NotificacionDTO evento) {
        try {
            // Crear la notificación en la base de datos
            Notificacion notificacion = new Notificacion();
            notificacion.setUsuarioId(evento.getUsuarioId());
            notificacion.setTitulo(evento.getTitulo());
            notificacion.setMensaje(evento.getMensaje());
            notificacion.setTipo(evento.getTipo());
            notificacion.setEstado("PENDIENTE");
            notificacion.setFechaCreacion(LocalDateTime.now());
            //notificacion.setRelacionadoId(evento.getRelacionadoId());
            notificacion.setTipoEntidad(evento.getTipoEntidad());

            notificacionRepository.persist(notificacion);

            // Enviar notificación según el tipo
            enviarNotificacion(notificacion, evento.getEmailUsuario());

            // Actualizar estado de la notificación
            notificacion.setEstado("ENVIADA");
            notificacion.setFechaEnvio(LocalDateTime.now());
            notificacionRepository.persist(notificacion);

        } catch (Exception e) {
            System.err.println("Error al procesar notificación: " + e.getMessage());
        }
    }

    private void enviarNotificacion(Notificacion notificacion, String email) {
        switch (notificacion.getTipo()) {
            case "EMAIL":
                enviarEmail(email, notificacion.getTitulo(), notificacion.getMensaje());
                break;
            case "PUSH":
                enviarPush(notificacion);
                break;
            case "SMS":
                enviarSMS(notificacion);
                break;
            case "INTERNA":
                // Las notificaciones internas solo se almacenan en BD
                break;
        }
    }

    private void enviarEmail(String destinatario, String asunto, String cuerpo) {
        try {
            mailer.send(Mail.withText(destinatario, asunto, cuerpo));
            System.out.println("Email enviado a: " + destinatario);
        } catch (Exception e) {
            System.err.println("Error al envia r email: " + e.getMessage());
        }
    }

    private void enviarPush(Notificacion notificacion) {
        // Implementación para enviar notificaciones push
        System.out.println("Enviando notificación PUSH al usuario ID: " + notificacion.getUsuarioId());
    }

    private void enviarSMS(Notificacion notificacion) {
        // Implementación para enviar SMS
        System.out.println("Enviando SMS al usuario ID: " + notificacion.getUsuarioId());
    }

    @Transactional
    public NotificacionDTO crearEventoNotificacion(Integer usuarioId, String titulo, String mensaje,
                                                      String tipo, Integer relacionadoId, String tipoEntidad) {

        try {
            // Obtener información del usuario
            UsuarioDTO usuario = usuarioRestClient.findById(usuarioId);

            NotificacionDTO evento = new NotificacionDTO();
            evento.setUsuarioId(usuarioId);
            evento.setEmailUsuario(usuario.getEmail());
            evento.setTitulo(titulo);
            evento.setMensaje(mensaje);
            evento.setTipo(tipo);
            //evento.setRelacionadoId(relacionadoId);
            evento.setTipoEntidad(tipoEntidad);
            evento.setFechaEvento(LocalDateTime.now());

            // Emitir evento a través del canal de notificaciones
            notificacionEmitter.send(evento);

            return evento;
        } catch (Exception e) {
            System.err.println("Error al crear evento de notificación: " + e.getMessage());
            return null;
        }
    }
}
