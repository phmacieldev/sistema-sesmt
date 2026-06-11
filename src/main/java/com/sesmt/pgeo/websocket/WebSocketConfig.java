/*
 * Copyright (c) 2026 Pedro Henrique Maciel da Silva Faria. Todos os direitos reservados.
 * Desenvolvido de forma independente como projeto de portfólio.
 * Autorizado apenas para uso interno homologado.
 */
package com.sesmt.pgeo.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuração do WebSocket (STOMP).
 *
 * Por que STOMP em vez de WebSocket puro?
 * - Suporte nativo no Spring
 * - Funciona com SockJS (fallback para redes que bloqueiam WS)
 * - Permite "canais" (tópicos) para broadcast seletivo
 *
 * Funcionamento:
 *   1. Cliente conecta em /ws via SockJS
 *   2. Assina o tópico /topic/agendamentos
 *   3. Quando qualquer usuário cria/edita/exclui um agendamento,
 *      o server faz broadcastAtualizacao() → todos os clientes recebem
 *      a mensagem e recarregam a tabela automaticamente
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Prefixo para tópicos de broadcast (server → todos os clientes)
        registry.enableSimpleBroker("/topic");
        // Prefixo para mensagens do cliente → server (não usado ainda)
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint de conexão WebSocket, com fallback SockJS
        registry.addEndpoint("/ws").withSockJS();
    }
}
