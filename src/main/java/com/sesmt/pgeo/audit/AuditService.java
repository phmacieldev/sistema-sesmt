package com.sesmt.pgeo.audit;

import com.sesmt.pgeo.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço central de auditoria.
 *
 * @Async garante que o log não atrasa a resposta ao usuário —
 * é gravado em background numa thread separada.
 *
 * @Transactional(REQUIRES_NEW) garante que o log é gravado
 * mesmo se a transação principal falhar (rollback não afeta o log).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepo;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(AuditLog auditLog) {
        try {
            auditLogRepo.save(auditLog);
        } catch (Exception e) {
            // Log de auditoria nunca deve derrubar a aplicação
            log.error("Falha ao gravar audit log: {}", e.getMessage());
        }
    }

    /** Atalho: registra ação com usuário da sessão atual */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(String acao, String entidade, Long entidadeId, String descricao) {
        AuditLog al = AuditLog.acao(getUsuarioAtual(), acao, entidade, entidadeId, descricao);
        registrar(al);
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarCriacao(String entidade, Long entidadeId, String descricao) {
        registrar(AuditLog.criar(getUsuarioAtual(), entidade, entidadeId, descricao));
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarEdicao(String entidade, Long entidadeId, String descricao,
                                String antes, String depois) {
        registrar(AuditLog.editar(getUsuarioAtual(), entidade, entidadeId, descricao, antes, depois));
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarExclusao(String entidade, Long entidadeId,
                                  String descricao, String dadosAntes) {
        registrar(AuditLog.excluir(getUsuarioAtual(), entidade, entidadeId, descricao, dadosAntes));
    }

    /** Retorna o username do usuário autenticado na sessão atual */
    public String getUsuarioAtual() {
        return SecurityUtils.getUsuarioAtual();
    }
}
