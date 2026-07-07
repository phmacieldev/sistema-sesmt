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

        List<Agendamento> todos = agendamentoRepo.findAll();
        Map<String, Integer> contadorTipo  = new LinkedHashMap<>();
        Map<String, Integer> contadorSetor = new LinkedHashMap<>();
        Map<String, Integer> contadorEstab = new LinkedHashMap<>();
        int[] contadorMes = new int[12];
        Set<Integer> anosDisponiveis = new TreeSet<>();
        int totalFiltrado = 0;

        for (Agendamento a : todos) {
            if (a.getDataClinico() == null) continue;
            int anoAg = a.getDataClinico().getYear();
            anosDisponiveis.add(anoAg);
            if (ano != null && ano != anoAg) continue;
            totalFiltrado++;
            int mes = a.getDataClinico().getMonthValue() - 1;
            String tipo  = a.getTipoExameDescricao();
            String setor = a.getFuncionarioSetor() != null && !a.getFuncionarioSetor().isBlank() ? a.getFuncionarioSetor() : "Não informado";
            String estab = "Não informado";
            try {
                if (a.getFuncionario() != null && a.getFuncionario().getEstabelecimentoEfetivo() != null
                        && !a.getFuncionario().getEstabelecimentoEfetivo().isBlank())
                    estab = a.getFuncionario().getEstabelecimentoEfetivo();
            } catch (Exception ignored) {}
            contadorMes[mes]++;
            contadorTipo.merge(tipo, 1, Integer::sum);
            contadorSetor.merge(setor, 1, Integer::sum);
            contadorEstab.merge(estab, 1, Integer::sum);
        }

        // Top 10 setores por volume
        Map<String, Integer> topSetores = contadorSetor.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(10)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                     (e1, e2) -> e1, LinkedHashMap::new));

        Map<String, Integer> topEstab = contadorEstab.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                     (e1, e2) -> e1, LinkedHashMap::new));

        model.addAttribute("contadorTipoExame",  contadorTipo);
        model.addAttribute("contadorExamesMes",  contadorMes);
        model.addAttribute("contadorSetor",      topSetores);
        model.addAttribute("contadorEstab",      topEstab);
        model.addAttribute("totalExames",        totalFiltrado);
        model.addAttribute("tiposLabels",        new ArrayList<>(contadorTipo.keySet()));
        model.addAttribute("tiposVals",          new ArrayList<>(contadorTipo.values()));
        model.addAttribute("setorLabels",        new ArrayList<>(topSetores.keySet()));
        model.addAttribute("setorVals",          new ArrayList<>(topSetores.values()));
        model.addAttribute("estabLabels",        new ArrayList<>(topEstab.keySet()));
        model.addAttribute("estabVals",          new ArrayList<>(topEstab.values()));
        model.addAttribute("meses", List.of("Jan","Fev","Mar","Abr","Mai","Jun",
                                            "Jul","Ago","Set","Out","Nov","Dez"));
        model.addAttribute("anosDisponiveis", new ArrayList<>(anosDisponiveis));
        model.addAttribute("anoFiltro", ano);
        return "dashboard_estatisticas";
    }

    private static final int ATESTADOS_POR_PAGINA = 15;

    @GetMapping("/indicadores")
    public String indicadores(
            @RequestParam(defaultValue = "60")  int  dias,
            @RequestParam(defaultValue = "0")   int  pagina,
            Model model) {

        LocalDate hoje     = LocalDate.now();
        LocalDate limite30 = hoje.plusDays(30);

        // ── Status dos ASOs ─────────────────────────────────────────────
        List<Funcionario> ativos = funcionarioRepo.findByAtivoTrue();
        long qtdVencidos = ativos.stream().filter(f -> f.getAso() != null && f.getAso().isBefore(hoje)).count();
        long qtdAVencer  = ativos.stream().filter(f -> f.getAso() != null && !f.getAso().isBefore(hoje) && f.getAso().isBefore(limite30)).count();
        long qtdEmDia    = ativos.stream().filter(f -> f.getAso() != null && !f.getAso().isBefore(limite30)).count();
        long qtdSemAso   = ativos.stream().filter(f -> f.getAso() == null).count();

        // ── Atestados por mês (últimos 12 meses) ────────────────────────
        LocalDate inicioPeriodo = hoje.minusMonths(11).withDayOfMonth(1);
        List<MedicalLeave> mlRecentes = medicalLeaveRepo.findAll().stream()
            .filter(ml -> ml.getDataAfastamento() != null && !ml.getDataAfastamento().isBefore(inicioPeriodo))
            .toList();

        List<String>  labelesMeses    = new ArrayList<>();
        List<Integer> atestadosPorMes = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM/yy", new java.util.Locale("pt", "BR"));
        for (int i = 11; i >= 0; i--) {
            LocalDate m = hoje.minusMonths(i).withDayOfMonth(1);
            labelesMeses.add(m.format(fmt));
            int anoM = m.getYear(), mesM = m.getMonthValue();
            atestadosPorMes.add((int) mlRecentes.stream()
                .filter(ml -> ml.getDataAfastamento().getYear() == anoM
                           && ml.getDataAfastamento().getMonthValue() == mesM)
                .count());
        }

        // ── Status dos agendamentos futuros ──────────────────────────────
        List<Agendamento> todosAg = agendamentoRepo.findAll();
        long agAgendados = todosAg.stream()
            .filter(a -> a.getDataClinico() != null && a.getDataClinico().isAfter(hoje))
            .count();
        long agEmDia = todosAg.stream()
            .filter(a -> a.getDataClinico() != null && !a.getDataClinico().isAfter(hoje)
                      && a.isAsoRecebido())
            .count();
        long agAtrasados = todosAg.stream()
            .filter(a -> a.getDataClinico() != null && a.getDataClinico().isBefore(hoje)
                      && !a.isAsoRecebido())
            .count();

        // ── Ranking de atestados (paginado) ─────────────────────────────
        LocalDate limiteDias = hoje.minusDays(dias);
        List<Map.Entry<String, Integer>> todosPorDias = medicalLeaveRepo.findRecentes(limiteDias)
            .stream()
            .collect(Collectors.groupingBy(
                ml -> ml.getFuncionario().getNome(),
                Collectors.summingInt(MedicalLeave::getDiasAfastamento)))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .toList();

        int totalItens    = todosPorDias.size();
        int totalPaginas  = totalItens == 0 ? 1 : (int) Math.ceil((double) totalItens / ATESTADOS_POR_PAGINA);
        int paginaSeg     = Math.max(0, Math.min(pagina, totalPaginas - 1));
        int from          = paginaSeg * ATESTADOS_POR_PAGINA;
        int to            = Math.min(from + ATESTADOS_POR_PAGINA, totalItens);
        List<Map.Entry<String, Integer>> pagina_ = todosPorDias.subList(from, to);

        model.addAttribute("qtdVencidos",   qtdVencidos);
        model.addAttribute("qtdAVencer",    qtdAVencer);
        model.addAttribute("qtdEmDia",      qtdEmDia);
        model.addAttribute("qtdSemAso",     qtdSemAso);
        model.addAttribute("totalAtivos",   ativos.size());
        model.addAttribute("labelesMeses",    labelesMeses);
        model.addAttribute("atestadosPorMes", atestadosPorMes);
        model.addAttribute("agAgendados",     agAgendados);
        model.addAttribute("agEmDia",         agEmDia);
        model.addAttribute("agAtrasados",     agAtrasados);
        model.addAttribute("resultados",    pagina_);
        model.addAttribute("totalItens",    totalItens);
        model.addAttribute("totalPaginas",  totalPaginas);
        model.addAttribute("paginaAtual",   paginaSeg);
        model.addAttribute("dias",          dias);
        return "indicadores";
    }

    @GetMapping("/vencimentos")
    public String vencimentosRedirect() {
        return "redirect:/dashboard_exames";
    }

    @GetMapping("/guias-semana")
    @ResponseBody
    public Map<String, Object> guiasSemana() {
        LocalDate hoje       = LocalDate.now();
        LocalDate amanha     = hoje.plusDays(1);
        LocalDate proxSegunda = hoje.with(DayOfWeek.MONDAY).plusWeeks(1);
        LocalDate proxSexta  = proxSegunda.plusDays(4);

        DateTimeFormatter dayFmt     = DateTimeFormatter.ofPattern("EEE dd/MM", Locale.of("pt", "BR"));
        DateTimeFormatter dataFmt    = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter rangeStart = DateTimeFormatter.ofPattern("dd/MM");

        // Sangue: exames de amanhã
        List<Map<String, Object>> sangueList = agendamentoRepo
            .findByDataSangueOrderByDataSangueAscHoraClinicoAsc(amanha)
            .stream()
            .map(a -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id",     a.getId());
                m.put("nome",   a.getFuncionarioNome()  != null ? a.getFuncionarioNome()  : "");
                m.put("setor",  a.getFuncionarioSetor() != null ? a.getFuncionarioSetor() : "");
                m.put("data",   a.getDataSangue().format(dayFmt));
                m.put("hora",   a.getHoraClinico()      != null ? a.getHoraClinico()      : "");
                m.put("exames", a.getExamesSangue()     != null ? a.getExamesSangue()     : "");
                return m;
            })
            .toList();

        // Clínico: exames da semana seguinte (seg–sex)
        List<Map<String, Object>> clinicoList = agendamentoRepo
            .findByDataClinicoBetweenOrderByDataClinicoAsc(proxSegunda, proxSexta)
            .stream()
            .map(a -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id",    a.getId());
                m.put("nome",  a.getFuncionarioNome()  != null ? a.getFuncionarioNome()  : "");
                m.put("setor", a.getFuncionarioSetor() != null ? a.getFuncionarioSetor() : "");
                m.put("data",  a.getDataClinico().format(dayFmt));
                m.put("hora",  a.getHoraClinico()      != null ? a.getHoraClinico()      : "");
                m.put("tipo",  a.getTipoExameDescricao());
                return m;
            })
            .toList();

        return Map.of(
            "periodoSangue",  amanha.format(dataFmt),
            "periodoClinico", proxSegunda.format(rangeStart) + " a " + proxSexta.format(dataFmt),
            "sangue",         sangueList,
            "clinico",        clinicoList
        );
    }

}
