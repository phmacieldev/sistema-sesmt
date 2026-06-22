/*
 * Copyright (c) 2026 Pedro Henrique Maciel da Silva Faria. Todos os direitos reservados.
 * Desenvolvido de forma independente como projeto de portfólio.
 * Autorizado apenas para uso interno homologado.
 */
package com.sesmt.pgeo.model;

import com.sesmt.pgeo.model.enums.TipoAtestado;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entidade MedicalLeave (Atestados Médicos).
 *
 * Equivalente ao modelo Flask:
 *   class MedicalLeave(db.Model):
 *       __tablename__ = "medical_leaves"
 *       ...
 */
@Entity
@Table(name = "medical_leaves", indexes = {
    @Index(name = "idx_ml_funcionario_id", columnList = "funcionario_id"),
    @Index(name = "idx_ml_data_afastamento", columnList = "data_afastamento")
})
@Data
@ToString(exclude = "funcionario")
@NoArgsConstructor
@AllArgsConstructor
public class MedicalLeave {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    // ManyToOne = muitos atestados podem pertencer a um funcionário
    // Equivalente ao db.ForeignKey("funcionario.id") do SQLAlchemy
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funcionario_id", nullable = false)
    private Funcionario funcionario;

    @Column(name = "data_afastamento", nullable = false)
    private LocalDate dataAfastamento;

    @Column(name = "dias_afastamento", nullable = false)
    private Integer diasAfastamento;

    @Column(length = 100)
    private String motivo;

    @Column(length = 10)
    private String cid;

    @Column(name = "medico_nome", length = 200)
    private String medicoNome;

    @Column(name = "medico_crm", length = 50)
    private String medicoCrm;

    @Column(name = "data_lancamento")
    private LocalDateTime dataLancamento = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private TipoAtestado tipo = TipoAtestado.NAO_INFORMADO;

    @PrePersist
    public void prePersist() {
        if (dataLancamento == null) {
            dataLancamento = LocalDateTime.now();
        }
    }
}
