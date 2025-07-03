package com.distribuida.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

@ApplicationScoped
public class RestClientConfig implements ClientHeadersFactory{

    @Inject
    JsonWebToken jwt;

    @Override
    public MultivaluedMap<String, String> update(
            MultivaluedMap<String, String> incomingHeaders,
            MultivaluedMap<String, String> clientOutgoingHeaders) {

        MultivaluedMap<String, String> result = new MultivaluedHashMap<>();
        result.putAll(clientOutgoingHeaders);

        // Propagar el token JWT automáticamente
        try {
            if (jwt != null && jwt.getRawToken() != null && !jwt.getRawToken().isEmpty()) {
                result.add("Authorization", "Bearer " + jwt.getRawToken());

                // Agregar headers adicionales útiles
                result.add("X-User-Id", jwt.getClaim("userId").toString());
                result.add("X-User-Role", jwt.getGroups().iterator().next());
                result.add("X-Request-Source", "microservice");

                System.out.println("JWT propagado - Usuario: " + jwt.getClaim("userId") +
                        " | Rol: " + jwt.getGroups().iterator().next());
            }
        } catch (Exception e) {
            System.out.println("Error al propagar JWT: " + e.getMessage());
        }

        return result;
    }
}
