package com.sesmt.pgeo.audit;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Log de auditoria — registra toda ação relevante no sistema.
 *
 * Cada vez que um agendamento é criado, editado ou excluído,
 * um registro é gravado aqui com: quem fez, o que fez, quando,
 * e os dados antes/depois (JSON simples).
 *
 * Atende NR-7 e demandas do DP para rastreabilidade.
 */
@Entity
@Table(name = "audit_log", indexes = {
    @Index(name = "idx_audit_entidade",   columnList = "entidade, entidade_id"),
    @Index(name = "idx_audit_usuario",    columnList = "usuario"),
    @Index(name = "idx_audit_criado_em",  columnList = "criado_em")
})
@Data
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Quem executou a ação */
    @Column(nullable = false, length = 50)
    private String usuario;

    /** Nome da entidade afetada: "Agendamento", "Funcionario", etc. */
    @Column(nullable = false, length = 50)
    private String entidade;

    /** ID do registro afetado */
    @Column(name = "entidade_id")
    private Long entidadeId;

    /**
     * Tipo de ação: CRIAR, EDITAR, EXCLUIR, LOGIN, LOGOUT,
     * ASO_ENVIADO, ASO_RECEBIDO, IMPORTAR_EXCEL, GERAR_PDF, etc.
     */
    @Column(nullable = false, length = 30)
    private String acao;

    /** Descrição resumida legível */
    @Column(length = 500)
    private String descricao;

    /** Estado anterior em JSON (null para CRIAR) */
    @Column(name = "dados_antes", columnDefinition = "TEXT")
    private String dadosAntes;

    /** Estado novo em JSON (null para EXCLUIR) */
    @Column(name = "dados_depois", columnDefinition = "TEXT")
    private String dadosDepois;

    /** IP do cliente */
    @Column(name = "ip_origem", length = 50)
    private String ipOrigem;

    @Column(name = "criado_em", updatable = false, nullable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    public void prePersist() {
        criadoEm = LocalDateTime.now();
    }

    // ── Factory methods ────────────────────────────────────────

    public static AuditLog criar(String usuario, String entidade, Long id, String descricao) {
        return build(usuario, entidade, id, "CRIAR", descricao, null, null, null);
    }

    public static AuditLog editar(String usuario, String entidade, Long id,
                                  String descricao, String antes, String depois) {
        return build(usuario, entidade, id, "EDITAR", descricao, antes, depois, null);
    }

    public static AuditLog excluir(String usuario, String entidade, Long id,
                                   String descricao, String dadosAntes) {
        return build(usuario, entidade, id, "EXCLUIR", descricao, dadosAntes, null, null);
    }

    public static AuditLog acao(String usuario, String acao, String entidade,
                                Long id, String descricao) {
        return build(usuario, entidade, id, acao, descricao, null, null, null);
    }

    private static AuditLog build(String usuario, String entidade, Long id, String acao,
                                  String descricao, String antes, String depois, String ip) {
        AuditLog log = new AuditLog();
        log.setUsuario(usuario);
        log.setEntidade(entidade);
        log.setEntidadeId(id);
        log.setAcao(acao);
        log.setDescricao(descricao);
        log.setDadosAntes(antes);
        log.setDadosDepois(depois);
        log.setIpOrigem(ip);
        return log;
    }
}
