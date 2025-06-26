package com.distribuida.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.distribuida.db.Proveedor;
import com.distribuida.db.RefreshToken;
import com.distribuida.db.Usuario;
import com.distribuida.dtos.*;
import com.distribuida.repo.RefreshTokenRepository;
import com.distribuida.repo.UsuarioRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@ApplicationScoped
@Transactional
public class AuthService {

    @Inject
    UsuarioRepository usuarioRepository;

    @Inject
    RefreshTokenRepository refreshTokenRepository;

    @Inject
    TokenService tokenService;

    public LoginResponse login(LoginRequest loginRequest) {
        // Buscar usuario por email
        Usuario usuario = usuarioRepository.find("email", loginRequest.getEmail())
                .firstResultOptional()
                .orElseThrow(() -> new NotAuthorizedException("Credenciales inválidas"));

        // Verificar que el usuario esté activo
        if (!usuario.getActivo()) {
            throw new NotAuthorizedException("Usuario inactivo");
        }

        // Verificar contraseña
        BCrypt.Result result = BCrypt.verifyer().verify(
                loginRequest.getPassword().toCharArray(),
                usuario.getPassword()
        );

        if (!result.verified) {
            throw new NotAuthorizedException("Credenciales inválidas");
        }

        // Generar tokens
        String accessToken = tokenService.generateAccessToken(usuario);
        String refreshTokenStr = tokenService.generateRefreshToken();

        // Guardar refresh token
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(refreshTokenStr);
        refreshToken.setUsuario(usuario);
        refreshToken.setFechaCreacion(LocalDateTime.now());
        refreshToken.setFechaExpiracion(LocalDateTime.now().plus(
                tokenService.getRefreshTokenDuration().toMillis(),
                ChronoUnit.MILLIS
        ));
        refreshToken.setUserAgent(loginRequest.getUserAgent());
        refreshToken.setIpAddress(loginRequest.getIpAddress());
        refreshToken.setActivo(true);

        refreshTokenRepository.persist(refreshToken);

        // Actualizar última conexión
        usuario.setFechaActualizacion(LocalDateTime.now());
        usuarioRepository.persist(usuario);

        // Crear respuesta
        LoginResponse response = new LoginResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshTokenStr);
        response.setExpiresIn(tokenService.getAccessTokenDuration().toSeconds());
        response.setUsuario(convertToDTO(usuario));

        return response;
    }

    public Usuario register(RegisterRequest registerRequest) {
        // Verificar que el email no esté en uso
        if (usuarioRepository.find("email", registerRequest.getEmail()).count() > 0) {
            throw new BadRequestException("El email ya está registrado");
        }

        // Crear nuevo usuario
        Usuario usuario = new Usuario();
        usuario.setNombre(registerRequest.getNombre());
        usuario.setApellido(registerRequest.getApellido());
        usuario.setEmail(registerRequest.getEmail());
        usuario.setTelefono(registerRequest.getTelefono());
        usuario.setDireccion(registerRequest.getDireccion());
        usuario.setRol(registerRequest.getRol());
        usuario.setActivo(true);
        usuario.setEmailVerificado(false);
        usuario.setFechaCreacion(LocalDateTime.now());
        usuario.setFechaActualizacion(LocalDateTime.now());

        // Encriptar contraseña
        String hashedPassword = BCrypt.withDefaults().hashToString(12,
                registerRequest.getPassword().toCharArray());
        usuario.setPassword(hashedPassword);

        // Si es proveedor, crear datos del proveedor
        if ("PROVEEDOR".equals(registerRequest.getRol()) &&
                registerRequest.getProveedor() != null) {
            usuario.setProveedor(convertToProveedor(registerRequest.getProveedor()));
        }

        usuarioRepository.persist(usuario);

        return usuario;
    }

    public LoginResponse refreshToken(RefreshTokenRequest request) {
        // Buscar refresh token
        RefreshToken refreshToken = refreshTokenRepository
                .find("token", request.getRefreshToken())
                .firstResultOptional()
                .orElseThrow(() -> new NotAuthorizedException("Token inválido"));

        // Verificar que esté activo y no expirado
        if (!refreshToken.getActivo() ||
                refreshToken.getFechaExpiracion().isBefore(LocalDateTime.now())) {
            throw new NotAuthorizedException("Token expirado o inactivo");
        }

        Usuario usuario = refreshToken.getUsuario();

        // Verificar que el usuario esté activo
        if (!usuario.getActivo()) {
            throw new NotAuthorizedException("Usuario inactivo");
        }

        // Invalidar token anterior
        refreshToken.setActivo(false);
        refreshTokenRepository.persist(refreshToken);

        // Generar nuevos tokens
        String accessToken = tokenService.generateAccessToken(usuario);
        String newRefreshTokenStr = tokenService.generateRefreshToken();

        // Guardar nuevo refresh token
        RefreshToken newRefreshToken = new RefreshToken();
        newRefreshToken.setToken(newRefreshTokenStr);
        newRefreshToken.setUsuario(usuario);
        newRefreshToken.setFechaCreacion(LocalDateTime.now());
        newRefreshToken.setFechaExpiracion(LocalDateTime.now().plus(
                tokenService.getRefreshTokenDuration().toMillis(),
                ChronoUnit.MILLIS
        ));
        newRefreshToken.setUserAgent(request.getUserAgent());
        newRefreshToken.setIpAddress(request.getIpAddress());
        newRefreshToken.setActivo(true);

        refreshTokenRepository.persist(newRefreshToken);

        // Crear respuesta
        LoginResponse response = new LoginResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(newRefreshTokenStr);
        response.setExpiresIn(tokenService.getAccessTokenDuration().toSeconds());
        response.setUsuario(convertToDTO(usuario));

        return response;
    }

    public void logout(String refreshToken) {
        refreshTokenRepository.find("token", refreshToken)
                .firstResultOptional()
                .ifPresent(token -> {
                    token.setActivo(false);
                    refreshTokenRepository.persist(token);
                });
    }

    public void changePassword(Integer userId, ChangePasswordRequest request) {
        Usuario usuario = usuarioRepository.findById(userId);
        if (usuario == null) {
            throw new NotFoundException("Usuario no encontrado");
        }

        // Verificar contraseña actual
        BCrypt.Result result = BCrypt.verifyer().verify(
                request.getCurrentPassword().toCharArray(),
                usuario.getPassword()
        );

        if (!result.verified) {
            throw new BadRequestException("Contraseña actual incorrecta");
        }

        // Actualizar contraseña
        String hashedPassword = BCrypt.withDefaults().hashToString(12,
                request.getNewPassword().toCharArray());
        usuario.setPassword(hashedPassword);
        usuario.setFechaActualizacion(LocalDateTime.now());

        usuarioRepository.persist(usuario);

        // Invalidar todos los refresh tokens del usuario
        refreshTokenRepository.find("usuario", usuario)
                .stream()
                .forEach(token -> {
                    token.setActivo(false);
                    refreshTokenRepository.persist(token);
                });
    }

    private UsuarioDTO convertToDTO(Usuario usuario) {
        UsuarioDTO dto = new UsuarioDTO();
        dto.setId(usuario.getId());
        dto.setNombre(usuario.getNombre());
        dto.setApellido(usuario.getApellido());
        dto.setEmail(usuario.getEmail());
        dto.setTelefono(usuario.getTelefono());
        dto.setDireccion(usuario.getDireccion());
        dto.setRol(usuario.getRol());
        dto.setFechaCreacion(usuario.getFechaCreacion());
        dto.setFechaActualizacion(usuario.getFechaActualizacion());

        if (usuario.getProveedor() != null) {
            ProveedorDto proveedorDto = new ProveedorDto();
            proveedorDto.setId(usuario.getProveedor().getId());
            proveedorDto.setNombreEmpresa(usuario.getProveedor().getNombreEmpresa());
            proveedorDto.setDescripcionEmpresa(usuario.getProveedor().getDescripcionEmpresa());
            proveedorDto.setMetodoPago(usuario.getProveedor().getMetodoPago());
            dto.setProveedor(proveedorDto);
        }

        return dto;
    }

    private Proveedor convertToProveedor(ProveedorDto dto) {
        Proveedor proveedor = new Proveedor();
        proveedor.setNombreEmpresa(dto.getNombreEmpresa());
        proveedor.setDescripcionEmpresa(dto.getDescripcionEmpresa());
        proveedor.setMetodoPago(dto.getMetodoPago());
        return proveedor;
    }
}
