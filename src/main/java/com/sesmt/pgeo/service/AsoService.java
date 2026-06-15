/*
 * Copyright (c) 2026 Pedro Henrique Maciel da Silva Faria. Todos os direitos reservados.
 * Desenvolvido de forma independente como projeto de portfólio.
 * Autorizado apenas para uso interno homologado.
 */
package com.sesmt.pgeo.service;

import com.sesmt.pgeo.audit.AuditService;
import com.sesmt.pgeo.exception.RecursoNaoEncontradoException;
import com.sesmt.pgeo.model.Agendamento;
import com.sesmt.pgeo.model.Funcionario;
import com.sesmt.pgeo.repository.AgendamentoRepository;
import com.sesmt.pgeo.repository.FuncionarioRepository;
import com.sesmt.pgeo.websocket.NotificacaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class AsoService {

    private final AgendamentoRepository agendamentoRepo;
    private final FuncionarioRepository funcionarioRepo;
    private final AuditService auditService;
    private final NotificacaoService notificacaoService;

    /**
     * Atualiza status de enviado/recebido do ASO.
     *
     * Bug corrigido: quando funcionário não tem matrícula (admissional
     * recém-cadastrado), buscava por matrícula nula e retornava false silenciosamente.
     * Agora usa a FK funcionario_id se disponível.
     */
    @Transactional
    public boolean atualizarStatusAso(Long agendamentoId, String campo, boolean valor,
                                      String usuarioAtual) {
        Agendamento ag = agendamentoRepo.findById(agendamentoId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento", agendamentoId));

        // Bug fix: usa FK (funcionario) primeiro, fallback pela matrícula
        Funcionario func = null;
        if (ag.getFuncionario() != null) {
            func = ag.getFuncionario();
        } else if (ag.getFuncionarioMatricula() != null) {
            func = funcionarioRepo.findByMatricula(ag.getFuncionarioMatricula()).orElse(null);
        }

        if ("enviado".equals(campo)) {
            ag.setAsoEnviado(valor);
            agendamentoRepo.save(ag);
            auditService.registrar(
                "ASO_ENVIADO", "Agendamento", agendamentoId,
                "ASO " + (valor ? "marcado como enviado" : "desmarcado") +
                " para " + ag.getFuncionarioNome());

        } else if ("guia_sangue".equals(campo)) {
            ag.setGuiaSangueEnviada(valor);
            agendamentoRepo.save(ag);
            auditService.registrar(
                "GUIA_SANGUE", "Agendamento", agendamentoId,
                "Guia de sangue " + (valor ? "marcada como enviada" : "desmarcada") +
                " para " + ag.getFuncionarioNome());

        } else if ("guia_clinico".equals(campo)) {
            ag.setGuiaClinicoEnviada(valor);
            agendamentoRepo.save(ag);
            auditService.registrar(
                "GUIA_CLINICO", "Agendamento", agendamentoId,
                "Guia clínico " + (valor ? "marcada como enviada" : "desmarcada") +
                " para " + ag.getFuncionarioNome());

        } else if ("recebido".equals(campo)) {
            if (valor) {
                // Guarda a data anterior para possível reversão
                if (!ag.isAsoRecebido() && func != null) {
                    ag.setDataAsoAnterior(func.getAso());
                }
                // Atualiza o ASO do funcionário para a data do exame clínico
                if (func != null && ag.getDataClinico() != null) {
                    func.setAso(ag.getDataClinico());
                    funcionarioRepo.save(func);
                }
                ag.setAsoRecebido(true);
                auditService.registrar("ASO_RECEBIDO", "Agendamento", agendamentoId,
                    "ASO de " + ag.getFuncionarioNome() + " marcado como recebido. " +
                    "Data ASO atualizada para " + ag.getDataClinico());
            } else {
                // Revertendo: restaura a data anterior (pode ser null se funcionário não tinha ASO)
                if (func != null) {
                    func.setAso(ag.getDataAsoAnterior());
                    funcionarioRepo.save(func);
                }
                ag.setDataAsoAnterior(null);
                ag.setAsoRecebido(false);
                auditService.registrar("ASO_RECEBIDO_REVERTIDO", "Agendamento", agendamentoId,
                    "Recebimento de ASO revertido para " + ag.getFuncionarioNome());
            }
            agendamentoRepo.save(ag);
            notificacaoService.broadcastAsoStatus(ag, usuarioAtual);
        }

        return true;
    }
}
