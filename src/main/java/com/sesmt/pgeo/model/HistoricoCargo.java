package com.sesmt.pgeo.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Rastreia toda alteração de cargo/função/setor de um funcionário.
 *
 * Casos de uso:
 *  - Mudança de risco: funcionário muda de cargo com risco diferente
 *    → novo exame MUDANCA_DE_RISCO é agendado
 *    → cargo antigo fica registrado aqui
 *  - Reversão: cargo volta ao estado anterior
 *    → registro com motivo=REVERSAO
 *  - Transferência simples sem mudança de risco
 */
@Entity
@Table(name = "historico_cargo")
@Data
@NoArgsConstructor
@ToString(exclude = "funcionario")
public class HistoricoCargo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funcionario_id", nullable = false)
    private Funcionario funcionario;

    @Column(name = "cargo_anterior", length = 100)
    private String cargoAnterior;

    @Column(name = "cargo_novo", length = 100)
    private String cargoNovo;

    @Column(name = "setor_anterior", length = 100)
    private String setorAnterior;

    @Column(name = "setor_novo", length = 100)
    private String setorNovo;

    /** MUDANCA_DE_RISCO | REVERSAO | TRANSFERENCIA | CORRECAO */
    @Column(length = 200)
    private String motivo;

    @Column(name = "alterado_por", length = 50)
    private String alteradoPor;

    @Column(name = "alterado_em", nullable = false)
    private LocalDateTime alteradoEm;

    @PrePersist
    public void prePersist() {
        if (alteradoEm == null) alteradoEm = LocalDateTime.now();
    }
}
