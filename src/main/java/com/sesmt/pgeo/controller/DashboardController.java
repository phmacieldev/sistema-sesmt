/*
 * Copyright (c) 2026 Pedro Henrique Maciel da Silva Faria. Todos os direitos reservados.
 * Desenvolvido de forma independente como projeto de portfólio.
 * Autorizado apenas para uso interno homologado.
 */
package com.sesmt.pgeo.controller;

import com.sesmt.pgeo.model.Agendamento;
import com.sesmt.pgeo.model.Funcionario;
import com.sesmt.pgeo.model.MedicalLeave;
import com.sesmt.pgeo.repository.AgendamentoRepository;
import com.sesmt.pgeo.repository.FuncionarioRepository;
import com.sesmt.pgeo.repository.MedicalLeaveRepository;
import com.sesmt.pgeo.service.ExcelService;
import com.sesmt.pgeo.service.GuiaSemanaService;
import com.sesmt.pgeo.service.IndicadorService;
import com.sesmt.pgeo.util.AppConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.Locale;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final AgendamentoRepository agendamentoRepo;
    private final FuncionarioRepository funcionarioRepo;
    private final MedicalLeaveRepository medicalLeaveRepo;
    private final ExcelService excelService;
    private final IndicadorService indicadorService;
    private final GuiaSemanaService guiaSemanaService;

    @GetMapping("/")
    public String root() { return "redirect:/home"; }

    @GetMapping("/dashboard")
    public String dashboard(
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data_inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data_fim,
            @RequestParam(required = false) String estabelecimento,
            Model model) {

        List<Integer> anos = agendamentoRepo.findAnosDisponiveis();
        if (anos.isEmpty()) anos = List.of(LocalDate.now().getYear());

        model.addAttribute("anos", anos);
        model.addAttribute("busca", busca);
        model.addAttribute("dataInicio", data_inicio);
        model.addAttribute("dataFim", data_fim);
        model.addAttribute("estabelecimento", estabelecimento);
        return "dashboard";
    }

    private static final int ITENS_POR_PAGINA = 20;

    @GetMapping("/dashboard_dados")
    public String dashboardDados(
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer ano,
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data_inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data_fim,
            @RequestParam(required = false) String estabelecimento,
            @RequestParam(required = false) String aplicar_filtro,
            @RequestParam(defaultValue = "0") int pagina,
            Model model) {

        String est = (estabelecimento != null && !estabelecimento.isBlank()
                      && !"todos".equalsIgnoreCase(estabelecimento))
                     ? estabelecimento.strip().toUpperCase() : null;
        String b = (busca != null && !busca.isBlank()) ? busca.strip() : null;

        PageRequest pageable = PageRequest.of(Math.max(0, pagina), ITENS_POR_PAGINA);
        Page<Agendamento> page;

        if ("true".equals(aplicar_filtro) && mes != null && ano != null
                && data_inicio == null && data_fim == null) {
            page = agendamentoRepo.findByMesEAnoPaginado(mes, ano, b, est, pageable);
        } else {
            page = agendamentoRepo.buscarPaginado(b, data_inicio, data_fim, est, pageable);
        }

        model.addAttribute("agendamentos", page.getContent());
        model.addAttribute("totalPaginas", page.getTotalPages());
        model.addAttribute("paginaAtual",  page.getNumber());
        model.addAttribute("totalItens",   page.getTotalElements());
        return "_tabela_agendamentos :: tbody";
    }

    @GetMapping("/exportar")
    public ResponseEntity<byte[]> exportar(
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer ano,
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data_inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data_fim,
            @RequestParam(required = false) String estabelecimento,
            @RequestParam(required = false) String aplicar_filtro) {

        String est = (estabelecimento != null && !estabelecimento.isBlank()
                      && !"todos".equalsIgnoreCase(estabelecimento))
                     ? estabelecimento.strip().toUpperCase() : null;
        String b = (busca != null && !busca.isBlank()) ? busca.strip() : null;

        List<Agendamento> lista;
        if ("true".equals(aplicar_filtro) && mes != null && ano != null
                && data_inicio == null && data_fim == null) {
            lista = agendamentoRepo.findByMesEAnoExport(mes, ano, b, est);
        } else {
            lista = agendamentoRepo.buscarTodosExport(b, data_inicio, data_fim, est);
        }

        byte[] bytes = excelService.gerarPlanilha(lista);
        String filename = "agendamentos_" + (mes != null ? mes + "_" : "") +
                          (ano != null ? ano : LocalDate.now().getYear()) + ".xlsx";

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(bytes);
    }

    @GetMapping("/dashboard_sangue")
    public String dashboardSangue(
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer ano,
            Model model) {

        LocalDate hoje = LocalDate.now();
        int mesFiltro = (mes != null) ? mes : hoje.getMonthValue();
        int anoFiltro = (ano != null) ? ano : hoje.getYear();

        List<Agendamento> agendamentos = agendamentoRepo.findByMesEAnoSangue(mesFiltro, anoFiltro);

        // Agrupa por dataSangue preservando ordem (TreeMap = ordem natural de LocalDate)
        Map<LocalDate, List<Agendamento>> porDataMap = new TreeMap<>();
        for (Agendamento a : agendamentos) {
            porDataMap.computeIfAbsent(a.getDataSangue(), k -> new ArrayList<>()).add(a);
        }

        DateTimeFormatter diaSemanaFmt = DateTimeFormatter
            .ofPattern("dd/MM/yyyy (EEEE)", Locale.of("pt", "BR"));

        List<Map<String, Object>> porDia = new ArrayList<>();
        for (Map.Entry<LocalDate, List<Agendamento>> entry : porDataMap.entrySet()) {
            int total = entry.getValue().size();
            Map<String, Object> dia = new LinkedHashMap<>();
            dia.put("data",           entry.getKey());
            dia.put("dataFormatada",  entry.getKey().format(diaSemanaFmt));
            dia.put("agendamentos",   entry.getValue());
            dia.put("total",          total);           // Integer — sem cast unsafe
            dia.put("cheio",          total >= AppConstants.LIMITE_SANGUE_DIA); // Boolean
            porDia.add(dia);
        }

        // Fix: comparação com Boolean em vez de cast (int) que causaria ClassCastException
        long diasCheios = porDia.stream()
            .filter(d -> Boolean.TRUE.equals(d.get("cheio")))
            .count();

        List<Integer> anos = agendamentoRepo.findAnosDisponiveis();
        if (anos.isEmpty()) anos = List.of(hoje.getYear());

        model.addAttribute("porDia",            porDia);
        model.addAttribute("mesFiltro",          mesFiltro);
        model.addAttribute("anoFiltro",          anoFiltro);
        model.addAttribute("anos",               anos);
        model.addAttribute("totalAgendamentos",  agendamentos.size());
        model.addAttribute("diasComAgendamento", porDia.size());
        model.addAttribute("diasCheios",         diasCheios);
        return "dashboard_sangue";
    }

    private static final int EXAMES_POR_PAGINA = 30;

    record MesTab(String key, String label, long count) {}

    @GetMapping("/dashboard_exames")
    public String dashboardExames(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) String estabelecimento,
            @RequestParam(defaultValue = "0") int pagina,
            Model model) {

        LocalDate hoje = LocalDate.now();

        List<Funcionario> todos = funcionarioRepo.findByAtivoTrue();

        if (busca != null && !busca.isBlank()) {
            String b = busca.strip().toLowerCase();
            todos = todos.stream()
                .filter(f -> f.getNome() != null && f.getNome().toLowerCase().contains(b))
                .toList();
        }
        if (estabelecimento != null && !estabelecimento.isBlank()
                && !"todos".equalsIgnoreCase(estabelecimento)) {
            String est = estabelecimento.strip().toUpperCase();
            todos = todos.stream()
                .filter(f -> est.equals(f.getEstabelecimentoEfetivo()))
                .toList();
        }

        long qtdVencidos = todos.stream().filter(f -> f.getAso() != null && f.getAso().isBefore(hoje)).count();
        long qtdEmDia    = todos.stream().filter(f -> f.getAso() != null && !f.getAso().isBefore(hoje)).count();
        long qtdSemAso   = todos.stream().filter(f -> f.getAso() == null).count();

        // Tabs mensais — todos os 12 meses do ano atual
        String[] nomeMeses = {"Jan","Fev","Mar","Abr","Mai","Jun","Jul","Ago","Set","Out","Nov","Dez"};
        List<MesTab> mesesTabs = new ArrayList<>();
        int anoAtual = hoje.getYear();
        for (int mes = 1; mes <= 12; mes++) {
            final int m = mes;
            long count = todos.stream()
                .filter(f -> f.getAso() != null
                    && f.getAso().getYear() == anoAtual
                    && f.getAso().getMonthValue() == m)
                .count();
            String key   = "mes_" + anoAtual + "_" + String.format("%02d", mes);
            String label = nomeMeses[mes - 1];
            mesesTabs.add(new MesTab(key, label, count));
        }

        String statusFiltro = status != null ? status : "todos";
        List<Funcionario> exames;
        if (statusFiltro.startsWith("mes_")) {
            String[] parts = statusFiltro.split("_");
            int ano = Integer.parseInt(parts[1]);
            int mes = Integer.parseInt(parts[2]);
            exames = todos.stream()
                .filter(f -> f.getAso() != null
                    && f.getAso().getYear() == ano
                    && f.getAso().getMonthValue() == mes)
                .toList();
        } else {
            exames = switch (statusFiltro) {
                case "vencidos" -> todos.stream().filter(f -> f.getAso() != null && f.getAso().isBefore(hoje)).toList();
                case "em_dia"   -> todos.stream().filter(f -> f.getAso() != null && !f.getAso().isBefore(hoje)).toList();
                case "sem_aso"  -> todos.stream().filter(f -> f.getAso() == null).toList();
                default         -> todos;
            };
        }

        exames = exames.stream()
            .sorted(Comparator.comparing(f -> f.getAso() != null ? f.getAso() : LocalDate.MAX))
            .toList();

        int totalItens   = exames.size();
        int totalPaginas = (int) Math.ceil((double) totalItens / EXAMES_POR_PAGINA);
        int paginaAtual  = Math.max(0, Math.min(pagina, Math.max(0, totalPaginas - 1)));
        int ini = paginaAtual * EXAMES_POR_PAGINA;
        List<Funcionario> examesPagina = exames.subList(ini, Math.min(ini + EXAMES_POR_PAGINA, totalItens));

        Map<Long, Long> diasAso = examesPagina.stream()
            .filter(f -> f.getAso() != null)
            .collect(Collectors.toMap(
                Funcionario::getId,
                f -> ChronoUnit.DAYS.between(hoje, f.getAso())));

        List<String> estabelecimentos = funcionarioRepo.findByAtivoTrue().stream()
            .map(Funcionario::getEstabelecimentoEfetivo)
            .filter(e -> e != null && !e.isBlank())
            .distinct().sorted().toList();

        model.addAttribute("exames",          examesPagina);
        model.addAttribute("hoje",            hoje);
        model.addAttribute("statusFiltro",    statusFiltro);
        model.addAttribute("busca",           busca);
        model.addAttribute("estabelecimento", estabelecimento);
        model.addAttribute("estabelecimentos", estabelecimentos);
        model.addAttribute("qtdTotal",        todos.size());
        model.addAttribute("qtdVencidos",     qtdVencidos);
        model.addAttribute("qtdEmDia",        qtdEmDia);
        model.addAttribute("qtdSemAso",       qtdSemAso);
        model.addAttribute("mesesTabs",       mesesTabs);
        model.addAttribute("diasAso",         diasAso);
        model.addAttribute("totalItens",      totalItens);
        model.addAttribute("totalPaginas",    totalPaginas);
        model.addAttribute("paginaAtual",     paginaAtual);
        return "dashboard_exames";
    }

    @GetMapping("/exportar_aso")
    public ResponseEntity<byte[]> exportarAso(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) String estabelecimento,
            @RequestParam(defaultValue = "30") int diasAviso) {

        LocalDate hoje   = LocalDate.now();
        LocalDate limite = hoje.plusDays(diasAviso);

        List<Funcionario> todos = funcionarioRepo.findByAtivoTrue();

        if (busca != null && !busca.isBlank()) {
            String b = busca.strip().toLowerCase();
            todos = todos.stream()
                .filter(f -> f.getNome() != null && f.getNome().toLowerCase().contains(b))
                .toList();
        }
        if (estabelecimento != null && !estabelecimento.isBlank()
                && !"todos".equalsIgnoreCase(estabelecimento)) {
            String est = estabelecimento.strip().toUpperCase();
            todos = todos.stream()
                .filter(f -> est.equals(f.getEstabelecimentoEfetivo()))
                .toList();
        }

        List<Funcionario> lista = switch (status != null ? status : "todos") {
            case "vencidos" -> todos.stream().filter(f -> f.getAso() != null && f.getAso().isBefore(hoje)).toList();
            case "a_vencer" -> todos.stream().filter(f -> f.getAso() != null && !f.getAso().isBefore(hoje) && f.getAso().isBefore(limite)).toList();
            case "em_dia"   -> todos.stream().filter(f -> f.getAso() != null && !f.getAso().isBefore(limite)).toList();
            case "sem_aso"  -> todos.stream().filter(f -> f.getAso() == null).toList();
            default         -> todos;
        };

        lista = lista.stream()
            .sorted(Comparator.comparing(f -> f.getAso() != null ? f.getAso() : LocalDate.MAX))
            .toList();

        byte[] bytes = excelService.gerarPlanilhaAso(lista, hoje, diasAviso);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"aso_vencimentos.xlsx\"")
            .contentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(bytes);
    }

    @GetMapping("/dashboard_estatisticas")
    public String dashboardEstatisticas(
            @RequestParam(required = false) Integer ano, Model model) {

        var est = indicadorService.calcularEstatisticas(ano);

        model.addAttribute("contadorTipoExame",  est.porTipo());
        model.addAttribute("contadorExamesMes",  est.porMes());
        model.addAttribute("contadorSetor",      est.porSetor());
        model.addAttribute("contadorEstab",      est.porEstabelecimento());
        model.addAttribute("totalExames",        est.total());
        model.addAttribute("tiposLabels",        new ArrayList<>(est.porTipo().keySet()));
        model.addAttribute("tiposVals",          new ArrayList<>(est.porTipo().values()));
        model.addAttribute("setorLabels",        new ArrayList<>(est.porSetor().keySet()));
        model.addAttribute("setorVals",          new ArrayList<>(est.porSetor().values()));
        model.addAttribute("estabLabels",        new ArrayList<>(est.porEstabelecimento().keySet()));
        model.addAttribute("estabVals",          new ArrayList<>(est.porEstabelecimento().values()));
        model.addAttribute("meses", List.of("Jan","Fev","Mar","Abr","Mai","Jun",
                                            "Jul","Ago","Set","Out","Nov","Dez"));
        model.addAttribute("anosDisponiveis", new ArrayList<>(est.anosDisponiveis()));
        model.addAttribute("anoFiltro", ano);
        return "dashboard_estatisticas";
    }

    private static final int ATESTADOS_POR_PAGINA = 15;

    @GetMapping("/indicadores")
    public String indicadores(
            @RequestParam(defaultValue = "60")  int  dias,
            @RequestParam(defaultValue = "0")   int  pagina,
            Model model) {

        var aso      = indicadorService.calcularStatusAso();
        var atMes    = indicadorService.calcularAtestadosPorMes();
        var agStatus = indicadorService.calcularStatusAgendamentos();
        var ranking  = indicadorService.calcularRanking(dias);

        int totalPaginas = ranking.totalItens() == 0 ? 1
            : (int) Math.ceil((double) ranking.totalItens() / ATESTADOS_POR_PAGINA);
        int paginaSeg = Math.max(0, Math.min(pagina, totalPaginas - 1));
        int from = paginaSeg * ATESTADOS_POR_PAGINA;
        int to   = Math.min(from + ATESTADOS_POR_PAGINA, ranking.totalItens());

        model.addAttribute("qtdVencidos",     aso.vencidos());
        model.addAttribute("qtdAVencer",      aso.aVencer());
        model.addAttribute("qtdEmDia",        aso.emDia());
        model.addAttribute("qtdSemAso",       aso.semAso());
        model.addAttribute("totalAtivos",     aso.totalAtivos());
        model.addAttribute("labelesMeses",    atMes.labels());
        model.addAttribute("atestadosPorMes", atMes.valores());
        model.addAttribute("agAgendados",     agStatus.agendados());
        model.addAttribute("agEmDia",         agStatus.emDia());
        model.addAttribute("agAtrasados",     agStatus.atrasados());
        model.addAttribute("resultados",      ranking.todos().subList(from, to));
        model.addAttribute("totalItens",      ranking.totalItens());
        model.addAttribute("totalPaginas",    totalPaginas);
        model.addAttribute("paginaAtual",     paginaSeg);
        model.addAttribute("dias",            dias);
        return "indicadores";
    }

    @GetMapping("/vencimentos")
    public String vencimentosRedirect() {
        return "redirect:/dashboard_exames";
    }

    @GetMapping("/guias-semana")
    @ResponseBody
    public com.sesmt.pgeo.dto.GuiasSemanaResponseDto guiasSemana() {
        return guiaSemanaService.montar();
    }

}
