package com.distribuida.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

@ApplicationScoped
public class JwtPropagationHeadersFactory implements ClientHeadersFactory {

    @Inject
    JsonWebToken jwt;

    @Override
    public MultivaluedMap<String, String> update(
            MultivaluedMap<String, String> incomingHeaders,
            MultivaluedMap<String, String> clientOutgoingHeaders) {

        MultivaluedMap<String, String> result = new MultivaluedHashMap<>();
        result.putAll(clientOutgoingHeaders);

        try {
            if (jwt != null && jwt.getRawToken() != null && !jwt.getRawToken().isEmpty()) {
                result.add("Authorization", "Bearer " + jwt.getRawToken());
            }
        } catch (Exception e) {
            // Silenciar - estamos en un hilo sin contexto RequestScoped
            // Los endpoints @PermitAll no necesitan JWT
        }

        return result;
    }
}
