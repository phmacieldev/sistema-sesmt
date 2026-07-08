/*
 * Copyright (c) 2026 Pedro Henrique Maciel da Silva Faria. Todos os direitos reservados.
 * Desenvolvido de forma independente como projeto de portfólio.
 * Autorizado apenas para uso interno homologado.
 */
package com.sesmt.pgeo.security;

import jakarta.servlet.http.HttpServletRequest;

/**
 * IP do cliente para rate limiting e auditoria.
 *
 * Usa request.getRemoteAddr() em vez de ler X-Forwarded-For diretamente:
 * com server.forward-headers-strategy=native (application-prod.properties),
 * o Tomcat já resolve esse valor via RemoteIpValve, só confiando no cabeçalho
 * quando a conexão de origem vem de um proxy interno conhecido (nginx/Railway).
 * Ler o header manualmente permitiria que o próprio cliente o falsificasse
 * para burlar o bloqueio de força bruta por IP.
 */
public final class ClientIpResolver {

    private ClientIpResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
