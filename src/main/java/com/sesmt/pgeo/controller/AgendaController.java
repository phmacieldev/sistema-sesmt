/*
 * Copyright (c) 2026 Pedro Henrique Maciel da Silva Faria. Todos os direitos reservados.
 * Desenvolvido de forma independente como projeto de portfólio.
 * Autorizado apenas para uso interno homologado.
 */
package com.sesmt.pgeo.controller;

import com.sesmt.pgeo.exception.RecursoNaoEncontradoException;
import com.sesmt.pgeo.exception.RegraDeNegocioException;
import com.sesmt.pgeo.model.Agendamento;
import com.sesmt.pgeo.model.enums.TipoExame;
import com.sesmt.pgeo.repository.AgendamentoRepository;
import com.sesmt.pgeo.repository.FuncionarioRepository;
import com.sesmt.pgeo.service.AgendamentoService;
import com.sesmt.pgeo.service.PdfService;
import com.sesmt.pgeo.util.AppConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Tag(name = "Agendamentos", description = "Criação, edição, calendário e APIs de suporte para agendamentos de exames")
@Controller
@RequiredArgsConstructor
public class AgendaController {

    private final AgendamentoRepository agendamentoRepo;
    private final FuncionarioRepository funcionarioRepo;
    private final AgendamentoService agendamentoService;
    private final PdfService pdfService;

    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ── Calendário ────────────────────────────────────────────────────

    @GetMapping("/agenda")
    public String agenda(Model model) {
        model.addAttribute("tiposExame", TipoExame.values());
        return "agenda";
    }

    @Operation(summary = "Lista eventos do calendário", description = "Retorna todos os agendamentos formatados para o FullCalendar")
    @GetMapping("/agenda_events_json")
    @ResponseBody
    public List<Map<String, Object>> agendaEventsJson() {
        return agendamentoRepo.findAllByOrderByDataClinicoAsc()
            .stream()
            .filter(a -> a.getDataClinico() != null && a.getHoraClinico() != null)
            .map(this::toEventoCalendario)
            .toList();
    }

    private Map<String, Object> toEventoCalendario(Agendamento a) {
        // Cor por tipo de exame — usa Enum, sem comparação de string solta
        String cor = switch (a.getTipoExame() != null ? a.getTipoExame() : TipoExame.PERIODICO) {
            case PERIODICO          -> "#27ae60";
            case ADMISSIONAL        -> "#2980b9";
            case DEMISSIONAL        -> "#e74c3c";
            case RETORNO_AO_TRABALHO-> "#f1c40f";
            case MUDANCA_DE_RISCO   -> "#ff8800";
        };

        String primeiroNome = a.getFuncionarioNome() != null
            ? a.getFuncionarioNome().split(" ")[0] : "?";

        Map<String, Object> ext = new LinkedHashMap<>();
        ext.put("nome",    a.getFuncionarioNome());
        ext.put("setor",   a.getFuncionarioSetor());
        ext.put("funcao",  a.getFuncionarioFuncao());
        ext.put("tipo",    a.getTipoExameDescricao());
        ext.put("sangue",  a.getDataSangue());
        ext.put("exigeSangue", a.isExigeSangue());

        Map<String, Object> ev = new LinkedHashMap<>();
        ev.put("id",            a.getId());
        ev.put("title",         primeiroNome + " | " + a.getTipoExameDescricao());
        ev.put("start",         a.getDataClinico() + "T" + a.getHoraClinico());
        ev.put("color",         cor);
        ev.put("extendedProps", ext);
        return ev;
    }

    // ── Formulário de agendamento ─────────────────────────────────────

    @GetMapping("/agendar")
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR')")
    public String agendar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(required = false) String matricula,
            Model model) {
        model.addAttribute("horarios",
            data != null ? agendamentoService.getHorariosDisponiveisPorData(data)
                         : agendamentoService.getHorariosDisponiveis());
        model.addAttribute("tiposExame", TipoExame.values());
        model.addAttribute("hoje", LocalDate.now().toString());
        if (matricula != null && !matricula.isBlank()) {
            funcionarioRepo.findByMatricula(matricula.strip()).ifPresent(f -> {
                model.addAttribute("prefillMatricula",  f.getMatricula() != null ? f.getMatricula() : "");
                model.addAttribute("prefillNome",       f.getNome() != null ? f.getNome() : "");
                model.addAttribute("prefillSetor",      f.getSetor() != null ? f.getSetor() : "");
                model.addAttribute("prefillFuncao",     f.getFuncao() != null ? f.getFuncao() : "");
                model.addAttribute("prefillExigeSangue", f.isExigeSangue());
            });
        }
        return "agendar";
    }

    /**
     * POST /agendar — retorna JSON.
     *
     * Bug fix: data_sangue agora é optional (required = false).
     * Bug fix: tipo_exame convertido para Enum antes de passar ao Service.
     */
    @Operation(summary = "Criar agendamento", description = "Cria um novo agendamento de exame. Retorna {ok, id} ou {duplicado, id} ou {erro, mensagem}")
    @PostMapping("/agendar")
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR')")
    @ResponseBody
    public Map<String, Object> agendarPost(
            @RequestParam(defaultValue = "")  String matricula,
            @RequestParam                     String nome,
            @RequestParam(defaultValue = "")  String setor,
            @RequestParam(defaultValue = "")  String funcao,
            @RequestParam                     String tipo_exame,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data_clinico,
            @RequestParam                     String hora,
            // Bug fix: required=false — cargos sem exame de sangue
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                              LocalDate data_sangue,
            @RequestParam(required = false) String observacoes) {

        if (data_clinico == null) {
            return Map.of("erro", true, "mensagem", "Data do exame clínico é obrigatória.");
        }
        TipoExame tipoEnum = TipoExame.fromDescricao(tipo_exame);
        if (tipoEnum == null) {
            return Map.of("erro", true, "mensagem", "Tipo de exame inválido: " + tipo_exame);
        }

        try {
            Agendamento ag = agendamentoService.criar(
                matricula, nome, setor, funcao, tipoEnum, data_clinico, hora, data_sangue, observacoes);
            return Map.of("ok", true, "id", ag.getId());

        } catch (RegraDeNegocioException ex) {
            String msg = ex.getMessage();
            // Duplicado: a mensagem começa com "DUPLICADO:{id}"
            if (msg.startsWith("DUPLICADO:")) {
                String[] partes = msg.split(" — ", 2);
                Long idExistente = Long.valueOf(partes[0].replace("DUPLICADO:", ""));
                return Map.of(
                    "duplicado", true,
                    "id",        idExistente,
                    "mensagem",  partes.length > 1 ? partes[1] : "Já possui agendamento"
                );
            }
            return Map.of("erro", true, "mensagem", msg);
        }
    }

    // ── Edição ────────────────────────────────────────────────────────

    @GetMapping("/editar_agendamento/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR')")
    public String editarForm(@PathVariable Long id,
                             @RequestParam(required = false) String origem,
                             Model model) {
        Agendamento ag = agendamentoRepo.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento", id));
        model.addAttribute("agendamento", ag);
        model.addAttribute("horarios", agendamentoService.getHorariosDisponiveis());
        model.addAttribute("tiposExame", TipoExame.values());
        model.addAttribute("origem", origem != null ? origem : "dashboard");
        return "editar_agendamento";
    }

    @PostMapping("/editar_agendamento/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR')")
    public String editarPost(
            @PathVariable Long id,
            @RequestParam String nome,
            @RequestParam String setor,
            @RequestParam String funcao,
            @RequestParam String tipo_exame,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data_clinico,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                            LocalDate data_sangue,
            @RequestParam String hora,
            @RequestParam(required = false) String observacoes,
            @RequestParam(defaultValue = "dashboard") String origem) {

        TipoExame tipoEnum = TipoExame.fromDescricao(tipo_exame);
        if (tipoEnum == null) tipoEnum = TipoExame.PERIODICO;

        agendamentoService.editar(id, nome, setor, funcao, tipoEnum, data_clinico, data_sangue, hora, observacoes);
        return "redirect:" + ("dashboard".equals(origem) ? "/dashboard" : "/agenda");
    }

    // ── Drag & drop ───────────────────────────────────────────────────

    @PostMapping("/mover_agendamento")
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR')")
    @ResponseBody
    public Map<String, Object> moverAgendamento(@RequestBody Map<String, Object> dados) {
        try {
            Long id       = Long.valueOf(dados.get("id").toString());
            LocalDate data = LocalDate.parse(dados.get("data").toString());
            String hora   = dados.get("hora").toString();
            agendamentoService.mover(id, data, hora);
            return Map.of("status", "ok");
        } catch (RegraDeNegocioException ex) {
            return Map.of("status", "erro", "mensagem", ex.getMessage());
        }
    }

    // ── Exclusão ──────────────────────────────────────────────────────

    /** Somente ADMIN pode excluir */
    @PostMapping("/excluir_agendamento/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String excluir(@PathVariable Long id) {
        agendamentoService.excluir(id);
        return "redirect:/dashboard";
    }

    // ── APIs de autocomplete ──────────────────────────────────────────

    @Operation(summary = "Buscar funcionário por matrícula")
    @GetMapping("/buscar_funcionario/{matricula}")
    @ResponseBody
    public Map<String, Object> buscarFuncionario(@PathVariable String matricula) {
        return funcionarioRepo.findByMatricula(matricula.strip())
            .map(f -> {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("encontrado",   true);
                r.put("nome",         f.getNome());
                r.put("setor",        f.getSetor());
                r.put("funcao",       f.getFuncao());
                r.put("exigeSangue",  f.isExigeSangue());
                r.put("estabelecimento", f.getEstabelecimentoEfetivo());
                return r;
            })
            .orElse(Map.of("encontrado", false));
    }

    @Operation(summary = "Autocomplete de funcionários por nome (mínimo 2 caracteres, máximo 10 resultados)")
    @GetMapping("/buscar_funcionarios_nome")
    @ResponseBody
    public List<Map<String, Object>> buscarPorNome(@RequestParam("q") String q) {
        if (q == null || q.strip().length() < 2) return List.of();
        return funcionarioRepo
            .findByNomeContainingIgnoreCaseOrderByNomeAsc(q.strip())
            .stream()
            .limit(10)
            .map(f -> {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("id",          f.getId());
                r.put("matricula",   f.getMatricula() != null ? f.getMatricula() : "");
                r.put("nome",        f.getNome());
                r.put("setor",       f.getSetor() != null ? f.getSetor() : "");
                r.put("funcao",      f.getFuncao() != null ? f.getFuncao() : "");
                r.put("exigeSangue", f.isExigeSangue());
                return r;
            })
            .toList();
    }

    @GetMapping("/buscar_funcionarios_matricula")
    @ResponseBody
    public List<Map<String, Object>> buscarPorMatricula(@RequestParam("q") String q) {
        if (q == null || q.strip().length() < 1) return List.of();
        return funcionarioRepo
            .findByMatriculaContainingIgnoreCaseOrderByNomeAsc(q.strip())
            .stream()
            .limit(10)
            .map(f -> {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("id",          f.getId());
                r.put("matricula",   f.getMatricula() != null ? f.getMatricula() : "");
                r.put("nome",        f.getNome());
                r.put("setor",       f.getSetor() != null ? f.getSetor() : "");
                r.put("funcao",      f.getFuncao() != null ? f.getFuncao() : "");
                r.put("exigeSangue", f.isExigeSangue());
                return r;
            })
            .toList();
    }

    @GetMapping("/setores")
    @ResponseBody
    public List<String> setores() {
        return funcionarioRepo.findDistinctSetores();
    }

    @GetMapping("/funcoes")
    @ResponseBody
    public List<String> funcoes() {
        return funcionarioRepo.findDistinctFuncoes();
    }

    @GetMapping("/verificar_agendamento_nome")
    @ResponseBody
    public Map<String, Object> verificarAgendamentoNome(@RequestParam String nome) {
        int ano = LocalDate.now().getYear();
        return agendamentoRepo.findByNomeEAno(nome.strip(), ano)
            .map(a -> {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("existe", true);
                r.put("id",     a.getId());
                r.put("data",   a.getDataClinico() != null ? a.getDataClinico().format(BR) : "—");
                r.put("hora",   a.getHoraClinico());
                return r;
            })
            .orElse(Map.of("existe", false));
    }

    // ── Verificar limite de sangue (para o formulário) ────────────────

    @Operation(summary = "Verifica disponibilidade de sangue para uma data")
    @GetMapping("/verificar_limite_sangue")
    @ResponseBody
    public Map<String, Object> verificarLimiteSangue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        long atual = agendamentoService.countSanguePorData(data);
        boolean atingido = atual >= AppConstants.LIMITE_SANGUE_DIA;
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("atual",    atual);
        r.put("limite",   AppConstants.LIMITE_SANGUE_DIA);
        r.put("atingido", atingido);
        r.put("vagas",    Math.max(0L, AppConstants.LIMITE_SANGUE_DIA - atual));
        return r;
    }

    // ── Detalhe do agendamento ────────────────────────────────────────

    @GetMapping("/agendamento/{id}")
    public String detalhe(@PathVariable Long id, Model model) {
        Agendamento ag = agendamentoRepo.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento", id));
        model.addAttribute("agendamento", ag);
        return "agendamento/detalhe";
    }

    @GetMapping("/guia/{id}")
    public ResponseEntity<byte[]> guia(@PathVariable Long id) {
        Agendamento ag = agendamentoRepo.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento", id));
        byte[] pdf = pdfService.gerarGuia(ag);
        String nomeArq = "guia_" + ag.getFuncionarioNome().replace(" ", "_") + ".pdf";
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nomeArq + "\"")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }

    @GetMapping("/guia/sangue/{id}")
    public ResponseEntity<byte[]> guiaSangue(@PathVariable Long id) {
        Agendamento ag = agendamentoRepo.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento", id));
        byte[] pdf = pdfService.gerarGuiaSangue(ag);
        String nomeArq = "guia_sangue_" + ag.getFuncionarioNome().replace(" ", "_") + ".pdf";
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + nomeArq + "\"")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }

    @GetMapping("/guia/clinico/{id}")
    public ResponseEntity<byte[]> guiaClinico(@PathVariable Long id) {
        Agendamento ag = agendamentoRepo.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento", id));
        byte[] pdf = pdfService.gerarGuiaClinico(ag);
        String nomeArq = "guia_clinico_" + ag.getFuncionarioNome().replace(" ", "_") + ".pdf";
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + nomeArq + "\"")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }
}
