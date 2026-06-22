/*
 * Copyright (c) 2026 Pedro Henrique Maciel da Silva Faria. Todos os direitos reservados.
 * Desenvolvido de forma independente como projeto de portfólio.
 * Autorizado apenas para uso interno homologado.
 */
package com.sesmt.pgeo.controller;

import com.sesmt.pgeo.exception.RegraDeNegocioException;
import com.sesmt.pgeo.model.Funcionario;
import com.sesmt.pgeo.model.enums.StatusFuncionario;
import com.sesmt.pgeo.service.FuncionarioImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.sesmt.pgeo.model.HistoricoCargo;
import com.sesmt.pgeo.repository.AgendamentoRepository;
import com.sesmt.pgeo.repository.FuncionarioRepository;
import com.sesmt.pgeo.repository.HistoricoCargoRepository;
import com.sesmt.pgeo.repository.MedicalLeaveRepository;
import com.sesmt.pgeo.service.FuncionarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;

@Tag(name = "Funcionários", description = "Perfil, alteração de cargo e gestão de funcionários")
@Controller
@RequiredArgsConstructor
public class FuncionarioController {

    private final FuncionarioRepository    funcionarioRepo;
    private final HistoricoCargoRepository historicoRepo;
    private final AgendamentoRepository    agendamentoRepo;
    private final MedicalLeaveRepository   atestadoRepo;
    private final FuncionarioService       funcionarioService;
    private final FuncionarioImportService importService;

    private static final int POR_PAGINA = 30;

    // ── Lista de funcionários ─────────────────────────────────────────

    @GetMapping("/funcionarios")
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR')")
    public String lista(@RequestParam(required = false) String nome,
                        @RequestParam(defaultValue = "0") int pagina,
                        Model model) {
        List<Funcionario> todos = (nome != null && !nome.isBlank())
            ? funcionarioRepo.findByNomeContainingIgnoreCaseOrderByNomeAsc(nome.strip())
            : funcionarioRepo.findAll(org.springframework.data.domain.Sort.by("nome"));

        int total    = todos.size();
        int totalPag = Math.max(1, (int) Math.ceil((double) total / POR_PAGINA));
        int pag      = Math.max(0, Math.min(pagina, totalPag - 1));
        int ini      = pag * POR_PAGINA;
        List<Funcionario> pagina_ = todos.subList(ini, Math.min(ini + POR_PAGINA, total));

        List<Funcionario> todosSemFiltro = funcionarioRepo.findAll(org.springframework.data.domain.Sort.by("nome"));
        long qtdAtivos        = todosSemFiltro.stream().filter(f -> f.getStatus() == StatusFuncionario.ATIVO).count();
        long qtdPreAdmissional = todosSemFiltro.stream().filter(f -> f.getStatus() == StatusFuncionario.PRE_ADMISSIONAL).count();
        long qtdDesligados    = todosSemFiltro.stream().filter(f -> f.getStatus() == StatusFuncionario.DESLIGADO).count();

        model.addAttribute("funcionarios",      pagina_);
        model.addAttribute("nomeFiltro",        nome);
        model.addAttribute("totalItens",        total);
        model.addAttribute("totalPaginas",      totalPag);
        model.addAttribute("paginaAtual",       pag);
        model.addAttribute("qtdAtivos",         qtdAtivos);
        model.addAttribute("qtdPreAdmissional", qtdPreAdmissional);
        model.addAttribute("qtdDesligados",     qtdDesligados);
        model.addAttribute("qtdTotal",          todosSemFiltro.size());
        return "funcionario/lista";
    }

    @GetMapping("/funcionarios/busca")
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR')")
    public String busca(@RequestParam(required = false) String nome, Model model) {
        List<Funcionario> resultado = (nome != null && !nome.isBlank())
            ? funcionarioRepo.findByNomeContainingIgnoreCaseOrderByNomeAsc(nome.strip())
            : List.of();
        model.addAttribute("funcionarios", resultado);
        return "funcionario/lista :: tabelaFuncionarios";
    }

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
        model.addAttribute("atestados",
            atestadoRepo.findByFuncionarioIdOrderByDataDesc(id));
        return "funcionario/perfil";
    }

    // ── Editar dados básicos do funcionário (somente ADMIN) ──────────

    @PostMapping("/funcionario/{id}/editar")
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public String editar(
            @PathVariable Long id,
            @RequestParam String nome,
            @RequestParam(required = false) String matricula,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String estabelecimento,
            @RequestParam(defaultValue = "true") boolean exigeSangue,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate aso,
            RedirectAttributes redirect) {
        Funcionario func = funcionarioRepo.findById(id)
            .orElseThrow(() -> new com.sesmt.pgeo.exception.RecursoNaoEncontradoException("Funcionário", id));
        if (nome != null && !nome.isBlank()) func.setNome(nome.strip());
        func.setMatricula(matricula != null && !matricula.isBlank() ? matricula.strip() : null);
        func.setEmail(email != null && !email.isBlank() ? email.strip() : null);
        func.setEstabelecimento(estabelecimento != null && !estabelecimento.isBlank() ? estabelecimento.strip() : null);
        func.setExigeSangue(exigeSangue);
        func.setAso(aso);
        funcionarioRepo.save(func);
        redirect.addFlashAttribute("mensagem", "Dados do funcionário atualizados.");
        return "redirect:/funcionario/" + id;
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

    @Operation(summary = "Buscar funcionário por matrícula (API JSON)")
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

    // ── Importação CSV / Excel (admin) ────────────────────────────────

    @GetMapping("/admin/funcionarios/importar")
    @PreAuthorize("hasRole('ADMIN')")
    public String importarForm() {
        return "admin/importar_funcionarios";
    }

    @PostMapping("/admin/funcionarios/importar")
    @PreAuthorize("hasRole('ADMIN')")
    public String importarPost(
            @RequestParam("arquivo") MultipartFile arquivo,
            RedirectAttributes redirect) {
        if (arquivo.isEmpty()) {
            redirect.addFlashAttribute("erro", "Nenhum arquivo selecionado.");
            return "redirect:/admin/funcionarios/importar";
        }
        try {
            var resultado = importService.importar(arquivo);
            redirect.addFlashAttribute("resultado", resultado);
        } catch (Exception e) {
            redirect.addFlashAttribute("erro", "Erro ao processar o arquivo. Verifique o formato e tente novamente.");
        }
        return "redirect:/admin/funcionarios/importar";
    }
}
