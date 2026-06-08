package com.sesmt.pgeo.controller;

import com.sesmt.pgeo.exception.RegraDeNegocioException;
import com.sesmt.pgeo.model.Funcionario;
import com.sesmt.pgeo.model.HistoricoCargo;
import com.sesmt.pgeo.repository.AgendamentoRepository;
import com.sesmt.pgeo.repository.FuncionarioRepository;
import com.sesmt.pgeo.repository.HistoricoCargoRepository;
import com.sesmt.pgeo.service.FuncionarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class FuncionarioController {

    private final FuncionarioRepository    funcionarioRepo;
    private final HistoricoCargoRepository historicoRepo;
    private final AgendamentoRepository    agendamentoRepo;
    private final FuncionarioService       funcionarioService;

    // ── Perfil do funcionário (histórico + agendamentos) ─────────────

    @GetMapping("/funcionario/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR')")
    public String perfil(@PathVariable Long id, Model model) {
        Funcionario func = funcionarioRepo.findById(id)
            .orElseThrow(() -> new com.sesmt.pgeo.exception
                .RecursoNaoEncontradoException("Funcionário", id));

        List<HistoricoCargo> historico = historicoRepo
            .findByFuncionarioIdOrderByAlteradoEmDesc(id);

        model.addAttribute("funcionario", func);
        model.addAttribute("historicoCargo", historico);
        model.addAttribute("agendamentos",
            agendamentoRepo.findByFuncionarioIdOrderByDataClinicoDesc(id));
        return "funcionario/perfil";
    }

    // ── Alterar cargo / mudança de risco ─────────────────────────────

    @PostMapping("/funcionario/{id}/alterar-cargo")
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR')")
    public String alterarCargo(
            @PathVariable Long id,
            @RequestParam String novoSetor,
            @RequestParam String novaFuncao,
            @RequestParam(defaultValue = "true") boolean exigeSangue,
            @RequestParam String motivo,
            RedirectAttributes redirect) {
        try {
            funcionarioService.alterarCargo(id, novoSetor, novaFuncao, exigeSangue, motivo);
            redirect.addFlashAttribute("mensagem",
                "Cargo atualizado. Histórico registrado.");
            // Se foi mudança de risco, sugere agendamento
            if ("MUDANCA_DE_RISCO".equals(motivo)) {
                redirect.addFlashAttribute("aviso",
                    "Lembre-se de agendar o exame de Mudança de Risco para este funcionário.");
            }
        } catch (RegraDeNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/funcionario/" + id;
    }

    // ── Efetivar admissional (vincular matrícula real) ────────────────

    @PostMapping("/funcionario/{id}/efetivar")
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR')")
    public String efetivar(
            @PathVariable Long id,
            @RequestParam(required = false) String matriculaReal,
            RedirectAttributes redirect) {
        try {
            funcionarioService.efetivarAdmissional(id, matriculaReal);
            redirect.addFlashAttribute("mensagem",
                "Funcionário efetivado com matrícula " + matriculaReal + ".");
        } catch (RegraDeNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/funcionario/" + id;
    }

    // ── API JSON: buscar funcionário com status ───────────────────────

    @GetMapping("/api/funcionario/{matricula}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR')")
    @ResponseBody
    public Map<String, Object> apiBuscar(@PathVariable String matricula) {
        return funcionarioRepo.findByMatricula(matricula.strip())
            .map(f -> Map.<String, Object>of(
                "encontrado",       true,
                "id",               f.getId(),
                "nome",             f.getNome(),
                "setor",            f.getSetor() != null ? f.getSetor() : "",
                "funcao",           f.getFuncao() != null ? f.getFuncao() : "",
                "exigeSangue",      f.isExigeSangue(),
                "status",           f.getStatus().name(),
                "preAdmissional",   f.isPreAdmissional(),
                "estabelecimento",  f.getEstabelecimentoEfetivo()
            ))
            .orElse(Map.of("encontrado", false));
    }
}
