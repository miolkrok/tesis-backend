package com.distribuida.repo;


import com.distribuida.db.RefreshToken;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;

@ApplicationScoped
@Transactional
public class RefreshTokenRepository implements PanacheRepositoryBase<RefreshToken, Integer> {

    public void deleteExpiredTokens() {
        delete("fechaExpiracion < ?1", LocalDateTime.now());
    }

    public void deleteInactiveTokens() {
        delete("activo = false");
    }

    public void invalidateUserTokens(Integer userId) {
        update("activo = false where usuario.id = ?1", userId);
    }
}
