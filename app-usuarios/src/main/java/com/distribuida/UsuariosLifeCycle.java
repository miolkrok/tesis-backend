package com.distribuida;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.vertx.mutiny.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import java.net.UnknownHostException;

@ApplicationScoped
public class UsuariosLifeCycle {

    public void init(@Observes StartupEvent evt, Vertx vertx) throws UnknownHostException {

    }

    public void stop(@Observes ShutdownEvent evt, Vertx vertx) {

    }
}
