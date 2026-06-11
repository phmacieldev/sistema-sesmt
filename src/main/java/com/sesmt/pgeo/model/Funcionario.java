package com.sesmt.pgeo.model;

import com.sesmt.pgeo.model.enums.StatusFuncionario;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "funcionario", indexes = {
    @Index(name = "idx_func_matricula", columnList = "matricula"),
    @Index(name = "idx_func_nome",      columnList = "nome"),
    @Index(name = "idx_func_status",    columnList = "status")
})
@Data
@ToString(exclude = {"atestados", "historicoCargos"})
@NoArgsConstructor
@AllArgsConstructor
public class Funcionario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    /**
     * Matrícula pode ser NULL para pré-admissionais.
     * Só é preenchida quando o funcionário entra no sistema da empresa
     * e a TI sincroniza os dados.
     */
    @Column(unique = true, length = 20)
    private String matricula;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(length = 100)
    private String setor;

    @Column(length = 100)
    private String funcao;

    @Column(length = 120)
    private String email;

    private LocalDate aso;

    @Column(length = 20)
    private String estabelecimento;

    @Column(name = "exige_sangue", nullable = false)
    private boolean exigeSangue = true;

    /**
     * ATIVO         → funcionário regular (dados vêm do banco da empresa)
     * PRE_ADMISSIONAL → cadastrado manualmente antes de entrar no sistema;
     *                   só tem nome/setor/função; sem matrícula
     * DESLIGADO     → inativo (demissional realizado)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusFuncionario status = StatusFuncionario.ATIVO;

    /** Mantido para compatibilidade com filtros. Derivado de status. */
    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @OneToMany(mappedBy = "funcionario", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MedicalLeave> atestados;

    @OneToMany(mappedBy = "funcionario", cascade = CascadeType.ALL,
               fetch = FetchType.LAZY, orphanRemoval = true)
    @OrderBy("alteradoEm DESC")
    private List<HistoricoCargo> historicoCargos;

    @PrePersist
    public void prePersist() {
        criadoEm = LocalDateTime.now();
        atualizadoEm = LocalDateTime.now();
        sincronizarAtivo();
        if (estabelecimento == null) estabelecimento = calcularEstabelecimento();
    }

    @PreUpdate
    public void preUpdate() {
        atualizadoEm = LocalDateTime.now();
        sincronizarAtivo();
    }

    /** Mantém o boolean 'ativo' sincronizado com o enum status */
    public void sincronizarAtivo() {
        this.ativo = (status != StatusFuncionario.DESLIGADO);
    }

    public String getEstabelecimentoEfetivo() {
        if (estabelecimento != null && !estabelecimento.isBlank())
            return estabelecimento.toUpperCase().strip();
        return calcularEstabelecimento();
    }

    private String calcularEstabelecimento() {
        if (setor == null || setor.isBlank()) return "HEAA";
        String s = setor.toLowerCase();
        if (s.contains("fmc"))  return "FMC";
        if (s.contains("csec")) return "CSEC";
        return "HEAA";
    }

    public boolean isPreAdmissional() {
        return status == StatusFuncionario.PRE_ADMISSIONAL;
    }
}
