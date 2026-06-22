/*
 * Copyright (c) 2026 Pedro Henrique Maciel da Silva Faria. Todos os direitos reservados.
 * Desenvolvido de forma independente como projeto de portfólio.
 * Autorizado apenas para uso interno homologado.
 */
package com.sesmt.pgeo.controller;

import com.sesmt.pgeo.exception.RecursoNaoEncontradoException;
import com.sesmt.pgeo.model.MedicalLeave;
import com.sesmt.pgeo.model.enums.TipoAtestado;
import com.sesmt.pgeo.repository.FuncionarioRepository;
import com.sesmt.pgeo.repository.MedicalLeaveRepository;
import com.sesmt.pgeo.service.AtestadoService;
import com.sesmt.pgeo.service.AtestadoPdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/atestados")
@RequiredArgsConstructor
public class AtestadoController {

    private final MedicalLeaveRepository repo;
    private final FuncionarioRepository funcRepo;
    private final AtestadoService service;
    private final AtestadoPdfService pdfService;

    private static final int POR_PAGINA = 20;

    private <T> List<T> paginar(List<T> lista, int pagina, Model model) {
        int total    = lista.size();
        int totalPag = Math.max(1, (int) Math.ceil((double) total / POR_PAGINA));
        int pag      = Math.max(0, Math.min(pagina, totalPag - 1));
        int ini      = pag * POR_PAGINA;
        model.addAttribute("totalPaginas", totalPag);
        model.addAttribute("paginaAtual",  pag);
        model.addAttribute("totalItens",   total);
        return lista.subList(ini, Math.min(ini + POR_PAGINA, total));
    }

    // ── Lista semanal ─────────────────────────────────────────────────

    @GetMapping
    public String lista(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate semana,
                        @RequestParam(required = false) String nome,
                        @RequestParam(defaultValue = "0") int pagina,
                        Model model) {
        LocalDate hoje = LocalDate.now();
        LocalDate inicio = semana != null ? service.semanaInicio(semana) : service.semanaInicio(hoje);
        LocalDate fim = service.semanaFim(inicio);

        boolean filtrando = nome != null && !nome.isBlank();
        List<MedicalLeave> todos = filtrando
            ? repo.findBySemanaENome(inicio, fim, nome.strip())
            : repo.findBySemanaOrdemLancamento(inicio, fim);
        Map<String, Integer> porSetor = service.totalPorSetor(todos);
        Map<String, Long>    porTipo  = service.totalPorTipo(todos);
        int totalDias = todos.stream().mapToInt(MedicalLeave::getDiasAfastamento).sum();

        model.addAttribute("atestados",        paginar(todos, pagina, model));
        model.addAttribute("semanaInicio",     inicio);
        model.addAttribute("semanaFim",        fim);
        model.addAttribute("semanaAnterior",   inicio.minusDays(7));
        model.addAttribute("semanaProxima",    inicio.plusDays(7));
        model.addAttribute("semanaAtualInicio", service.semanaInicio(hoje));
        model.addAttribute("porSetor",         porSetor);
        model.addAttribute("porTipo",          porTipo);
        model.addAttribute("totalDias",        totalDias);
        model.addAttribute("totalAtestados",   todos.size());
        model.addAttribute("nomeFiltro",       nome);
        return "atestados/lista";
    }

    // ── Formulário ────────────────────────────────────────────────────

    @GetMapping("/novo")
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR')")
    public String novoForm(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate semana,
                           Model model) {
        LocalDate inicio = semana != null ? semana : service.semanaInicio(LocalDate.now());
        model.addAttribute("tipos",        TipoAtestado.values());
        model.addAttribute("semanaInicio", inicio);
        model.addAttribute("atestado",     new MedicalLeave());
        model.addAttribute("edicao",       false);
        return "atestados/form";
    }

    @PostMapping("/novo")
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR')")
    public String novoPost(
            @RequestParam Long funcionarioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataAfastamento,
            @RequestParam Integer diasAfastamento,
            @RequestParam(required = false) String motivo,
            @RequestParam(required = false) String cid,
            @RequestParam(required = false) String medicoNome,
            @RequestParam(required = false) String medicoCrm,
            @RequestParam TipoAtestado tipo) {

        MedicalLeave ml = new MedicalLeave();
        ml.setFuncionario(funcRepo.findById(funcionarioId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Funcionario", funcionarioId)));
        ml.setDataAfastamento(dataAfastamento);
        ml.setDiasAfastamento(diasAfastamento);
        ml.setMotivo(motivo);
        ml.setCid(cid != null ? cid.toUpperCase().strip() : null);
        ml.setMedicoNome(medicoNome);
        ml.setMedicoCrm(medicoCrm);
        ml.setTipo(tipo);
        repo.save(ml);

        LocalDate inicio = service.semanaInicio(dataAfastamento);
        return "redirect:/atestados?semana=" + inicio;
    }

    @GetMapping("/{id}/editar")
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR')")
    public String editarForm(@PathVariable Long id, Model model) {
        MedicalLeave ml = repo.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Atestado", id));
        model.addAttribute("atestado",     ml);
        model.addAttribute("tipos",        TipoAtestado.values());
        model.addAttribute("semanaInicio", service.semanaInicio(ml.getDataAfastamento()));
        model.addAttribute("edicao",       true);
        return "atestados/form";
    }

    @PostMapping("/{id}/editar")
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR')")
    public String editarPost(
            @PathVariable Long id,
            @RequestParam Long funcionarioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataAfastamento,
            @RequestParam Integer diasAfastamento,
            @RequestParam(required = false) String motivo,
            @RequestParam(required = false) String cid,
            @RequestParam(required = false) String medicoNome,
            @RequestParam(required = false) String medicoCrm,
            @RequestParam TipoAtestado tipo) {

        MedicalLeave ml = repo.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Atestado", id));
        ml.setFuncionario(funcRepo.findById(funcionarioId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Funcionario", funcionarioId)));
        ml.setDataAfastamento(dataAfastamento);
        ml.setDiasAfastamento(diasAfastamento);
        ml.setMotivo(motivo);
        ml.setCid(cid != null ? cid.toUpperCase().strip() : null);
        ml.setMedicoNome(medicoNome);
        ml.setMedicoCrm(medicoCrm);
        ml.setTipo(tipo);
        repo.save(ml);

        return "redirect:/atestados?semana=" + service.semanaInicio(dataAfastamento);
    }

    @PostMapping("/{id}/excluir")
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR')")
    public String excluir(@PathVariable Long id,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate semana) {
        MedicalLeave ml = repo.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Atestado", id));
        LocalDate inicio = service.semanaInicio(ml.getDataAfastamento());
        repo.delete(ml);
        return "redirect:/atestados?semana=" + inicio;
    }

    // ── Busca global por nome (fragmento para live search) ───────────

    @GetMapping("/busca")
    public String busca(@RequestParam(required = false) String nome, Model model) {
        if (nome == null || nome.isBlank()) {
            model.addAttribute("atestados", List.of());
        } else {
            model.addAttribute("atestados", repo.findByNomeTodos(nome.strip()));
        }
        return "atestados/lista :: tabelaAtestados";
    }

    // ── Indicadores 60 dias ───────────────────────────────────────────

    @GetMapping("/indicadores")
    public String indicadores(@RequestParam(defaultValue = "0") int pagina, Model model) {
        LocalDate limite = LocalDate.now().minusDays(60);
        List<MedicalLeave> todos = repo.findUltimos60Dias(limite);
        List<AtestadoService.ResumoFuncionario> resumos = service.resumoPor60Dias(todos);
        long comRisco = resumos.stream().filter(AtestadoService.ResumoFuncionario::isRiscoInss).count();
        int totalDias = todos.stream().mapToInt(MedicalLeave::getDiasAfastamento).sum();

        model.addAttribute("resumos",        paginar(resumos, pagina, model));
        model.addAttribute("comRisco",        comRisco);
        model.addAttribute("totalDias",       totalDias);
        model.addAttribute("totalAtestados",  todos.size());
        model.addAttribute("limite",          limite);
        return "atestados/indicadores";
    }

    // ── API JSON para o modal ─────────────────────────────────────────

    @GetMapping("/api/tipos")
    @ResponseBody
    public List<Map<String, String>> apiTipos() {
        return Arrays.stream(TipoAtestado.values())
            .map(t -> Map.of("value", t.name(), "label", t.getDescricao()))
            .toList();
    }

    @GetMapping("/{id}/json")
    @ResponseBody
    public Map<String, Object> atestadoJson(@PathVariable Long id) {
        MedicalLeave ml = repo.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Atestado", id));
        java.util.LinkedHashMap<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("id",               ml.getId());
        m.put("funcionarioId",    ml.getFuncionario() != null ? ml.getFuncionario().getId() : null);
        m.put("funcionarioNome",  ml.getFuncionario() != null ? ml.getFuncionario().getNome() : "");
        m.put("dataAfastamento",  ml.getDataAfastamento() != null ? ml.getDataAfastamento().toString() : "");
        m.put("diasAfastamento",  ml.getDiasAfastamento());
        m.put("tipo",             ml.getTipo() != null ? ml.getTipo().name() : "");
        m.put("cid",              ml.getCid() != null ? ml.getCid() : "");
        m.put("medicoNome",       ml.getMedicoNome() != null ? ml.getMedicoNome() : "");
        m.put("medicoCrm",        ml.getMedicoCrm() != null ? ml.getMedicoCrm() : "");
        return m;
    }

    @PostMapping("/novo/modal")
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR')")
    @ResponseBody
    public Map<String, Object> novoModal(
            @RequestParam Long funcionarioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataAfastamento,
            @RequestParam Integer diasAfastamento,
            @RequestParam(required = false) String cid,
            @RequestParam(required = false) String medicoNome,
            @RequestParam(required = false) String medicoCrm,
            @RequestParam TipoAtestado tipo) {
        try {
            MedicalLeave ml = new MedicalLeave();
            ml.setFuncionario(funcRepo.findById(funcionarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Funcionario", funcionarioId)));
            ml.setDataAfastamento(dataAfastamento);
            ml.setDiasAfastamento(diasAfastamento);
            ml.setCid(cid != null ? cid.toUpperCase().strip() : null);
            ml.setMedicoNome(medicoNome);
            ml.setMedicoCrm(medicoCrm);
            ml.setTipo(tipo);
            repo.save(ml);
            return Map.of("ok", true);
        } catch (Exception e) {
            return Map.of("ok", false, "mensagem", e.getMessage());
        }
    }

    @PostMapping("/{id}/editar/modal")
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR')")
    @ResponseBody
    public Map<String, Object> editarModal(
            @PathVariable Long id,
            @RequestParam Long funcionarioId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataAfastamento,
            @RequestParam Integer diasAfastamento,
            @RequestParam(required = false) String cid,
            @RequestParam(required = false) String medicoNome,
            @RequestParam(required = false) String medicoCrm,
            @RequestParam TipoAtestado tipo) {
        try {
            MedicalLeave ml = repo.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Atestado", id));
            ml.setFuncionario(funcRepo.findById(funcionarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Funcionario", funcionarioId)));
            ml.setDataAfastamento(dataAfastamento);
            ml.setDiasAfastamento(diasAfastamento);
            ml.setCid(cid != null ? cid.toUpperCase().strip() : null);
            ml.setMedicoNome(medicoNome);
            ml.setMedicoCrm(medicoCrm);
            ml.setTipo(tipo);
            repo.save(ml);
            return Map.of("ok", true);
        } catch (Exception e) {
            return Map.of("ok", false, "mensagem", e.getMessage());
        }
    }

    // ── PDF ───────────────────────────────────────────────────────────

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> pdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate semana) {
        LocalDate inicio = semana != null ? service.semanaInicio(semana) : service.semanaInicio(LocalDate.now());
        LocalDate fim = service.semanaFim(inicio);
        List<MedicalLeave> atestados = repo.findBySemana(inicio, fim);
        Map<String, Integer> porSetor = service.totalPorSetor(atestados);
        Map<String, Long>    porTipo  = service.totalPorTipo(atestados);

        byte[] pdf = pdfService.gerarRelatorio(atestados, inicio, fim, porSetor, porTipo);
        String nome = "atestados_" + inicio + "_a_" + fim + ".pdf";
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nome + "\"")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }
}
