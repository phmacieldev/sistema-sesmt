/*
 * Copyright (c) 2026 Pedro Henrique Maciel da Silva Faria. Todos os direitos reservados.
 * Desenvolvido de forma independente como projeto de portfólio.
 * Autorizado apenas para uso interno homologado.
 */
package com.sesmt.pgeo.controller;

import com.sesmt.pgeo.dto.*;
import com.sesmt.pgeo.exception.RecursoNaoEncontradoException;
import com.sesmt.pgeo.exception.RegraDeNegocioException;
import com.sesmt.pgeo.model.Agendamento;
import com.sesmt.pgeo.model.enums.TipoExame;
import jakarta.validation.Valid;
import com.sesmt.pgeo.repository.AgendamentoRepository;
import com.sesmt.pgeo.repository.FuncionarioRepository;
import com.sesmt.pgeo.service.AgendamentoService;
import com.sesmt.pgeo.service.PdfService;
import com.sesmt.pgeo.util.AppConstants;
import io.sentry.Sentry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Arrays;

@Slf4j
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
        model.addAttribute("horarios", agendamentoService.getHorariosDisponiveis());
        model.addAttribute("hoje", LocalDate.now().toString());
        return "agenda";
    }

    @GetMapping("/api/tipos-exame")
    @ResponseBody
    public List<String> apiTiposExame() {
        return Arrays.stream(TipoExame.values())
            .map(TipoExame::getDescricao)
            .toList();
    }

    @GetMapping("/api/horarios")
    @ResponseBody
    public List<String> apiHorarios() {
        return agendamentoService.getHorariosDisponiveis();
    }

    @Operation(summary = "Lista eventos do calendário", description = "Retorna agendamentos recentes/futuros para o FullCalendar")
    @GetMapping("/agenda_events_json")
    @ResponseBody
    public List<CalendarioEventoDto> agendaEventsJson() {
        LocalDate inicio = LocalDate.now().minusMonths(3);
        return agendamentoRepo.findByDataClinicoDesde(inicio)
            .stream()
            .filter(a -> a.getHoraClinico() != null)
            .map(CalendarioEventoDto::fromEntity)
            .toList();
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
    public ApiResponseDto agendarPost(@Valid CreateAgendamentoDto dto) {
        TipoExame tipoEnum = TipoExame.fromDescricao(dto.tipo_exame());
        if (tipoEnum == null) {
            return ApiResponseDto.erro("Tipo de exame inválido: " + dto.tipo_exame());
        }

        try {
            Agendamento ag = agendamentoService.criar(
                dto.matricula(), dto.nome(), dto.setor(), dto.funcao(),
                tipoEnum, dto.data_clinico(), dto.hora(), dto.data_sangue(),
                dto.observacoes(), dto.exames_sangue());
            return ApiResponseDto.sucesso(ag.getId());

        } catch (RegraDeNegocioException ex) {
            String msg = ex.getMessage();
            if (msg.startsWith("DUPLICADO:")) {
                String[] partes = msg.split(" — ", 2);
                Long idExistente = Long.valueOf(partes[0].replace("DUPLICADO:", ""));
                return ApiResponseDto.duplicado(idExistente,
                    partes.length > 1 ? partes[1] : "Já possui agendamento");
            }
            return ApiResponseDto.erro(msg);
        } catch (Exception ex) {
            log.error("Erro ao criar agendamento", ex);
            Sentry.captureException(ex);
            return ApiResponseDto.erro("Erro interno ao criar agendamento.");
        }
    }

    // ── Edição ────────────────────────────────────────────────────────

    @GetMapping("/agendamento/{id}/json")
    @ResponseBody
    public AgendamentoResponseDto agendamentoJson(@PathVariable Long id) {
        Agendamento ag = agendamentoRepo.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento", id));
        return AgendamentoResponseDto.fromEntity(ag);
    }

    @PostMapping("/editar_agendamento/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR')")
    @ResponseBody
    public ApiResponseDto editarPost(@PathVariable Long id, @Valid UpdateAgendamentoDto dto) {
        TipoExame tipoEnum = TipoExame.fromDescricao(dto.tipo_exame());
        if (tipoEnum == null) tipoEnum = TipoExame.PERIODICO;

        try {
            agendamentoService.editar(id, dto.nome(), dto.setor(), dto.funcao(),
                tipoEnum, dto.data_clinico(), dto.data_sangue(), dto.hora(),
                dto.observacoes(), dto.exames_sangue());
            return ApiResponseDto.sucesso();
        } catch (RegraDeNegocioException ex) {
            return ApiResponseDto.erro(ex.getMessage());
        } catch (Exception ex) {
            log.error("Erro ao editar agendamento {}", id, ex);
            Sentry.captureException(ex);
            return ApiResponseDto.erro("Erro ao atualizar agendamento.");
        }
    }

    // ── Drag & drop ───────────────────────────────────────────────────

    @PostMapping("/mover_agendamento")
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR')")
    @ResponseBody
    public Map<String, Object> moverAgendamento(@Valid @RequestBody MoverAgendamentoDto dto) {
        try {
            agendamentoService.mover(dto.id(), LocalDate.parse(dto.data()), dto.hora());
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
    public FuncionarioBuscaDto buscarFuncionario(@PathVariable String matricula) {
        return funcionarioRepo.findByMatricula(matricula.strip())
            .map(FuncionarioBuscaDto::encontrado)
            .orElse(FuncionarioBuscaDto.naoEncontrado());
    }

    @Operation(summary = "Autocomplete de funcionários por nome (mínimo 2 caracteres, máximo 10 resultados)")
    @GetMapping("/buscar_funcionarios_nome")
    @ResponseBody
    public List<FuncionarioResumoDto> buscarPorNome(@RequestParam("q") String q) {
        if (q == null || q.strip().length() < 2) return List.of();
        // Limite aplicado no banco (Pageable) — .limit(10) em stream carregaria todos
        return funcionarioRepo
            .buscarPorNomeLimitado(q.strip(), org.springframework.data.domain.PageRequest.of(0, 10))
            .stream()
            .map(FuncionarioResumoDto::fromEntity)
            .toList();
    }

    @GetMapping("/buscar_funcionarios_matricula")
    @ResponseBody
    public List<FuncionarioResumoDto> buscarPorMatricula(@RequestParam("q") String q) {
        if (q == null || q.strip().length() < 1) return List.of();
        // Limite aplicado no banco (Pageable) — .limit(10) em stream carregaria todos
        return funcionarioRepo
            .findByMatriculaContainingIgnoreCaseOrderByNomeAsc(
                q.strip(), org.springframework.data.domain.PageRequest.of(0, 10))
            .stream()
            .map(FuncionarioResumoDto::fromEntity)
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
    public LimiteSangueResponseDto verificarLimiteSangue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        long atual = agendamentoService.countSanguePorData(data);
        boolean atingido = atual >= AppConstants.LIMITE_SANGUE_DIA;
        return new LimiteSangueResponseDto(atingido, atual);
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
        String nomeArq = "guia_" + sanitizarNomeArquivo(ag.getFuncionarioNome()) + ".pdf";
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                .filename(nomeArq, StandardCharsets.UTF_8).build().toString())
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }

    @GetMapping("/guia/sangue/{id}")
    public ResponseEntity<byte[]> guiaSangue(@PathVariable Long id) {
        Agendamento ag = agendamentoRepo.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento", id));
        byte[] pdf = pdfService.gerarGuiaSangue(ag);
        String nomeArq = "guia_sangue_" + sanitizarNomeArquivo(ag.getFuncionarioNome()) + ".pdf";
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                .filename(nomeArq, StandardCharsets.UTF_8).build().toString())
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }

    @GetMapping("/guia/clinico/{id}")
    public ResponseEntity<byte[]> guiaClinico(@PathVariable Long id) {
        Agendamento ag = agendamentoRepo.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento", id));
        byte[] pdf = pdfService.gerarGuiaClinico(ag);
        String nomeArq = "guia_clinico_" + sanitizarNomeArquivo(ag.getFuncionarioNome()) + ".pdf";
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                .filename(nomeArq, StandardCharsets.UTF_8).build().toString())
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }

    /** Restringe o nome a caracteres seguros para uso em filename de header HTTP. */
    private static String sanitizarNomeArquivo(String nome) {
        if (nome == null || nome.isBlank()) return "funcionario";
        return nome.strip().replaceAll("[^\\p{L}\\p{N}]+", "_");
    }
}
