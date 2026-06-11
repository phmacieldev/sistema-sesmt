package com.sesmt.pgeo.model;

import com.sesmt.pgeo.model.enums.TipoExame;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entidade Agendamento.
 *
 * Fase 1 — mudanças críticas:
 *
 * 1. FK real para Funcionario (@ManyToOne) em vez de dados denormalizados.
 *    Isso garante consistência quando o funcionário muda de setor/função.
 *    Os campos funcionarioNome/Setor/Funcao foram MANTIDOS como cache
 *    para exibição rápida no dashboard sem JOIN, mas são atualizados via
 *    @PrePersist/@PreUpdate automaticamente.
 *
 * 2. TipoExame agora é um Enum em vez de String livre — elimina bug de
 *    comparação case-sensitive ("Periódico" vs "periódico" vs "PERIODICO").
 *
 * 3. dataSangue agora é nullable explicitamente — alguns cargos não exigem.
 *
 * 4. criadoEm / atualizadoEm para auditoria e histórico.
 *
 * 5. criadoPor referencia o usuario que fez o agendamento (para logs futuros).
 */
@Entity
@Table(name = "agendamento", indexes = {
    @Index(name = "idx_ag_data_clinico",   columnList = "data_clinico"),
    @Index(name = "idx_ag_funcionario_id", columnList = "funcionario_id"),
    @Index(name = "idx_ag_nome_cache",     columnList = "funcionario_nome")
})
@Data
@ToString(exclude = "funcionario")
@NoArgsConstructor
@AllArgsConstructor
public class Agendamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    // ── FK real ───────────────────────────────────────────────
    /**
     * Referência ao Funcionario.
     * FetchType.LAZY = não carrega o funcionário junto por padrão
     * (evita N+1 queries no dashboard com muitos agendamentos).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funcionario_id")
    private Funcionario funcionario;

    // ── Cache desnormalizado (para exibição rápida sem JOIN) ──
    // Atualizados automaticamente via syncCacheDoFuncionario()
    @Column(name = "funcionario_nome",   length = 120)
    private String funcionarioNome;

    @Column(name = "funcionario_setor",  length = 100)
    private String funcionarioSetor;

    @Column(name = "funcionario_funcao", length = 100)
    private String funcionarioFuncao;

    @Column(name = "funcionario_matricula", length = 20)
    private String funcionarioMatricula;

    // ── Dados do exame ────────────────────────────────────────
    /**
     * Tipo de exame como Enum — elimina bugs de string.
     * Salvo como String no banco (EnumType.STRING) para legibilidade.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_exame", length = 30)
    private TipoExame tipoExame;

    @Column(name = "data_clinico")
    private LocalDate dataClinico;

    @Column(name = "hora_clinico", length = 10)
    private String horaClinico;

    /**
     * Nullable — nem todos os cargos exigem exame de sangue.
     * Antes esse campo causava NullPointerException na validação.
     */
    @Column(name = "data_sangue")
    private LocalDate dataSangue;

    // ── Status ASO ────────────────────────────────────────────
    @Column(name = "aso_enviado", nullable = false)
    private boolean asoEnviado = false;

    @Column(name = "aso_recebido", nullable = false)
    private boolean asoRecebido = false;

    @Column(name = "data_aso_anterior")
    private LocalDate dataAsoAnterior;

    // ── Auditoria ─────────────────────────────────────────────
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    /** Username de quem criou (para logs/auditoria futura) */
    @Column(name = "criado_por", length = 50, updatable = false)
    private String criadoPor;

    @Column(name = "atualizado_por", length = 50)
    private String atualizadoPor;

    /** Campo livre para anotações do operador */
    @Column(columnDefinition = "TEXT")
    private String observacoes;

    @PrePersist
    public void prePersist() {
        criadoEm = LocalDateTime.now();
        atualizadoEm = LocalDateTime.now();
        syncCacheDoFuncionario();
    }

    @PreUpdate
    public void preUpdate() {
        atualizadoEm = LocalDateTime.now();
        syncCacheDoFuncionario();
    }

    /**
     * Mantém o cache sincronizado com os dados atuais do Funcionario.
     * Se o funcionário não estiver carregado (LAZY), usa os valores já no cache.
     */
    public void syncCacheDoFuncionario() {
        if (funcionario != null) {
            this.funcionarioNome      = funcionario.getNome();
            this.funcionarioSetor     = funcionario.getSetor();
            this.funcionarioFuncao    = funcionario.getFuncao();
            this.funcionarioMatricula = funcionario.getMatricula();
        }
    }

    // ── Helpers ───────────────────────────────────────────────

    /**
     * Retorna o estabelecimento: primeiro tenta pelo Funcionario (FK),
     * depois pelo cache do setor.
     *
     * Bug corrigido: antes usava só o setor da string desnormalizada,
     * ignorando o campo estabelecimento do Funcionario.
     */
    public String getEstabelecimento() {
        if (funcionario != null) {
            return funcionario.getEstabelecimentoEfetivo();
        }
        // fallback pelo cache do setor
        if (funcionarioSetor == null || funcionarioSetor.isBlank()) return "HEAA";
        String s = funcionarioSetor.toLowerCase();
        if (s.contains("fmc"))  return "FMC";
        if (s.contains("csec")) return "CSEC";
        return "HEAA";
    }

    /** Descrição legível do tipo de exame (para templates) */
    public String getTipoExameDescricao() {
        return tipoExame != null ? tipoExame.getDescricao() : "—";
    }

    /** True se este agendamento exige exame de sangue */
    public boolean isExigeSangue() {
        if (funcionario != null) return funcionario.isExigeSangue();
        return dataSangue != null; // fallback: se tem data, exigia
    }
}
