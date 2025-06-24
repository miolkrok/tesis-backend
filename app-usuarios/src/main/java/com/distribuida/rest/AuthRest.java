package com.distribuida.rest;

import com.distribuida.dtos.*;
import com.distribuida.service.AuthService;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.Set;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class AuthRest {

    @Inject
    AuthService authService;

    @Inject
    JsonWebToken jwt;

    @POST
    @Path("/login")
    @PermitAll
    public Response login(@Valid LoginRequest loginRequest,
                          @HeaderParam("User-Agent") String userAgent,
                          @HeaderParam("X-Forwarded-For") String ipAddress) {

        loginRequest.setUserAgent(userAgent);
        loginRequest.setIpAddress(ipAddress);

        LoginResponse response = authService.login(loginRequest);
        return Response.ok(response).build();
    }

    @POST
    @Path("/register")
    @PermitAll
    public Response register(@Valid RegisterRequest registerRequest) {
        authService.register(registerRequest);
        return Response.status(Response.Status.CREATED)
                .entity(new MessageResponse("Usuario registrado exitosamente"))
                .build();
    }

    @POST
    @Path("/refresh")
    @PermitAll
    public Response refreshToken(@Valid RefreshTokenRequest request,
                                 @HeaderParam("User-Agent") String userAgent,
                                 @HeaderParam("X-Forwarded-For") String ipAddress) {

        request.setUserAgent(userAgent);
        request.setIpAddress(ipAddress);

        LoginResponse response = authService.refreshToken(request);
        return Response.ok(response).build();
    }

    @POST
    @Path("/logout")
    @Authenticated
    public Response logout(@Valid RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return Response.ok(new MessageResponse("Sesión cerrada exitosamente")).build();
    }

    @POST
    @Path("/change-password")
    @Authenticated
    public Response changePassword(@Valid ChangePasswordRequest request,
                                   @Context SecurityContext securityContext) {

        Integer userId = Integer.valueOf(jwt.getClaim("userId"));
        authService.changePassword(userId, request);

        return Response.ok(new MessageResponse("Contraseña actualizada exitosamente"))
                .build();
    }

    @GET
    @Path("/me")
    @Authenticated
    public Response getCurrentUser() {
        return Response.ok(new UserInfoResponse(
                Integer.valueOf(jwt.getClaim("userId")),
                jwt.getName(),
                jwt.getClaim("nombre"),
                jwt.getClaim("apellido"),
                jwt.getGroups()
        )).build();
    }
}

@Data
@AllArgsConstructor
class MessageResponse {
    private String message;
}

@Data
@AllArgsConstructor
class UserInfoResponse {
    private Integer userId;
    private String email;
    private String nombre;
    private String apellido;
    private Set<String> roles;
}


