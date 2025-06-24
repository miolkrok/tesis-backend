package com.distribuida.scheduler;

import com.distribuida.repo.RefreshTokenRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import io.quarkus.scheduler.Scheduled;

@ApplicationScoped
public class TokenCleanupScheduler {

    @Inject
    RefreshTokenRepository refreshTokenRepository;

    @Scheduled(every = "24h")
    @Transactional
    public void cleanupExpiredTokens() {
        System.out.println("Limpiando tokens expirados...");
        refreshTokenRepository.deleteExpiredTokens();
        refreshTokenRepository.deleteInactiveTokens();
    }
}
