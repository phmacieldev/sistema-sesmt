/*
 * Copyright (c) 2026 Pedro Henrique Maciel da Silva Faria. Todos os direitos reservados.
 * Desenvolvido de forma independente como projeto de portfólio.
 * Autorizado apenas para uso interno homologado.
 */
package com.sesmt.pgeo.websocket;

import com.sesmt.pgeo.model.Agendamento;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Serviço de notificações em tempo real via WebSocket.
 *
 * Chamado pelos Services de negócio após cada operação relevante.
 * Todos os clientes conectados ao tópico /topic/agendamentos recebem
 * a notificação e atualizam a UI automaticamente.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacaoService {

    private final SimpMessagingTemplate messagingTemplate;

    private static final String TOPICO_AGENDAMENTOS = "/topic/agendamentos";

    public void broadcastCriacao(Agendamento ag, String usuarioAtual) {
        broadcast(AgendamentoNotificacao.de(ag, "CRIAR", usuarioAtual));
    }

    public void broadcastEdicao(Agendamento ag, String usuarioAtual) {
        broadcast(AgendamentoNotificacao.de(ag, "EDITAR", usuarioAtual));
    }

    public void broadcastExclusao(Long id, String nome, String usuarioAtual) {
        broadcast(AgendamentoNotificacao.exclusao(id, nome, usuarioAtual));
    }

    public void broadcastAsoStatus(Agendamento ag, String usuarioAtual) {
        broadcast(AgendamentoNotificacao.de(ag, "ASO_STATUS", usuarioAtual));
    }

    private void broadcast(AgendamentoNotificacao notificacao) {
        try {
            messagingTemplate.convertAndSend(TOPICO_AGENDAMENTOS, notificacao);
            log.debug("Notificação WebSocket enviada: {}", notificacao.getMensagem());
        } catch (Exception e) {
            // Não deixar falha de WS derrubar a operação principal
            log.error("Falha ao enviar notificação WebSocket: {}", e.getMessage());
        }
    }
}
