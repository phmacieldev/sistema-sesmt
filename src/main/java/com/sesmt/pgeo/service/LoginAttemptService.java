package com.sesmt.pgeo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rastreia tentativas de login por IP para prevenir brute force.
 * Bloqueia o IP após MAX_TENTATIVAS falhas dentro de JANELA_MS.
 * A contagem é zerada automaticamente ao expirar ou no login bem-sucedido.
 */
@Slf4j
@Service
public class LoginAttemptService {

    private static final int MAX_TENTATIVAS = 10;
    private static final long JANELA_MS = 15 * 60 * 1_000L; // 15 minutos

    private record Tentativas(AtomicInteger contador, Instant inicio) {}

    private final Map<String, Tentativas> mapa = new ConcurrentHashMap<>();

    public void registrarFalha(String ip) {
        mapa.compute(ip, (key, atual) -> {
            if (atual == null || expirou(atual)) {
                return new Tentativas(new AtomicInteger(1), Instant.now());
            }
            int total = atual.contador().incrementAndGet();
            if (total >= MAX_TENTATIVAS) {
                log.warn("RATE LIMIT | ip={} | tentativas={}", ip, total);
            }
            return atual;
        });
    }

    public void registrarSucesso(String ip) {
        mapa.remove(ip);
    }

    public boolean estaBloqueado(String ip) {
        Tentativas t = mapa.get(ip);
        if (t == null) return false;
        if (expirou(t)) {
            mapa.remove(ip);
            return false;
        }
        return t.contador().get() >= MAX_TENTATIVAS;
    }

    private boolean expirou(Tentativas t) {
        return Instant.now().isAfter(t.inicio().plusMillis(JANELA_MS));
    }
}
