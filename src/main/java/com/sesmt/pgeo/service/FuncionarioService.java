package com.sesmt.pgeo.service;

import com.sesmt.pgeo.audit.AuditService;
import com.sesmt.pgeo.exception.RegraDeNegocioException;
import com.sesmt.pgeo.exception.RecursoNaoEncontradoException;
import com.sesmt.pgeo.model.Funcionario;
import com.sesmt.pgeo.model.HistoricoCargo;
import com.sesmt.pgeo.model.enums.StatusFuncionario;
import com.sesmt.pgeo.repository.FuncionarioRepository;
import com.sesmt.pgeo.repository.HistoricoCargoRepository;
import com.sesmt.pgeo.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;

@Service
@RequiredArgsConstructor
public class FuncionarioService {

    private final FuncionarioRepository    funcionarioRepo;
    private final HistoricoCargoRepository historicoRepo;
    private final AuditService             auditService;

    /**
     * Altera cargo e/ou setor de um funcionário registrando histórico.
     *
     * Motivos possíveis:
     *   MUDANCA_DE_RISCO  → cargo muda e o risco NR-7 muda → novo exame deve ser agendado
     *   REVERSAO          → cargo volta ao estado anterior (risco revertido)
     *   TRANSFERENCIA     → mudança de setor sem mudança de risco
     *   CORRECAO          → ajuste de dado errado, sem impacto em ASO
     */
    @Transactional
    public Funcionario alterarCargo(Long funcionarioId,
                                    String novoSetor,
                                    String novaFuncao,
                                    boolean exigeSangue,
                                    String motivo) {

        Funcionario func = funcionarioRepo.findById(funcionarioId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Funcionário", funcionarioId));

        // Registra histórico antes de alterar
        HistoricoCargo hist = new HistoricoCargo();
        hist.setFuncionario(func);
        hist.setCargoAnterior(func.getFuncao());
        hist.setCargoNovo(novaFuncao);
        hist.setSetorAnterior(func.getSetor());
        hist.setSetorNovo(novoSetor);
        hist.setMotivo(motivo);
        hist.setAlteradoPor(getUsuarioAtual());
        historicoRepo.save(hist);

        // Captura o cargo anterior ANTES de alterar (para auditoria correta)
        String cargoAnteriorAudit = func.getFuncao();

        // Aplica alteração
        func.setSetor(novoSetor);
        func.setFuncao(novaFuncao);
        func.setExigeSangue(exigeSangue);
        // Recalcula estabelecimento se o setor mudou
        func.setEstabelecimento(null); // força recálculo no @PreUpdate

        Funcionario salvo = funcionarioRepo.save(func);

        auditService.registrar("ALTERACAO_CARGO", "Funcionario", funcionarioId,
            func.getNome() + " | " + motivo + " | " +
            cargoAnteriorAudit + " → " + novaFuncao);

        return salvo;
    }

    /**
     * Cria um pré-admissional — funcionário que ainda não está no banco da empresa
     * mas precisa do agendamento de exame antes de ser contratado.
     *
     * Matrícula: gerada automaticamente com prefixo ADM, provisória.
     * A TI depois vincula ao funcionário real quando ele entrar no sistema.
     */
    @Transactional
    public Funcionario criarPreAdmissional(String nome, String setor,
                                           String funcao, boolean exigeSangue) {
        if (nome == null || nome.isBlank()) {
            throw new RegraDeNegocioException("Nome é obrigatório para pré-admissional.");
        }

        Funcionario func = new Funcionario();
        func.setNome(nome.strip());
        func.setSetor(setor);
        func.setFuncao(funcao);
        func.setExigeSangue(exigeSangue);
        func.setStatus(StatusFuncionario.PRE_ADMISSIONAL);
        func.setAtivo(true);
        func.setMatricula(gerarMatriculaAdmissional());

        Funcionario salvo = funcionarioRepo.save(func);

        auditService.registrarCriacao("Funcionario", salvo.getId(),
            "Pré-admissional criado: " + nome + " | " + funcao);

        return salvo;
    }

    /**
     * Quando o admissional é efetivado, a TI pode vincular a matrícula real.
     * O status muda de PRE_ADMISSIONAL para ATIVO.
     */
    @Transactional
    public Funcionario efetivarAdmissional(Long funcionarioId, String matriculaReal) {
        Funcionario func = funcionarioRepo.findById(funcionarioId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Funcionário", funcionarioId));

        if (func.getStatus() != StatusFuncionario.PRE_ADMISSIONAL) {
            throw new RegraDeNegocioException("Este funcionário não está como pré-admissional.");
        }

        // Verifica se a matrícula já existe
        if (matriculaReal != null && !matriculaReal.isBlank()) {
            funcionarioRepo.findByMatricula(matriculaReal.strip()).ifPresent(outro -> {
                if (!outro.getId().equals(funcionarioId)) {
                    throw new RegraDeNegocioException(
                        "Matrícula " + matriculaReal + " já está em uso.");
                }
            });
            func.setMatricula(matriculaReal.strip());
        }

        func.setStatus(StatusFuncionario.ATIVO);
        func.setAtivo(true);
        Funcionario salvo = funcionarioRepo.save(func);

        auditService.registrar("EFETIVAR_ADMISSIONAL", "Funcionario", funcionarioId,
            func.getNome() + " efetivado com matrícula " + func.getMatricula());

        return salvo;
    }

    private String gerarMatriculaAdmissional() {
        int ano = Year.now().getValue();
        long proximo = funcionarioRepo.count() + 1;
        return String.format("ADM%d%04d", ano, proximo);
    }

    private static String getUsuarioAtual() {
        return SecurityUtils.getUsuarioAtual();
    }
}
