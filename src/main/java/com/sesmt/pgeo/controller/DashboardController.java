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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final AgendamentoRepository agendamentoRepo;
    private final FuncionarioRepository funcionarioRepo;
    private final MedicalLeaveRepository medicalLeaveRepo;
    private final ExcelService excelService;

    @GetMapping("/")
    public String home() { return "redirect:/dashboard"; }

    @GetMapping("/dashboard")
    public String dashboard(
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data_inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data_fim,
            @RequestParam(required = false) String estabelecimento,
            Model model) {

        List<Integer> anos = agendamentoRepo.findAnosDisponiveis();
        if (anos.isEmpty()) anos = List.of(LocalDate.now().getYear());

        String buscaFiltro = (busca != null && !busca.isBlank()) ? busca.strip() : null;
        List<Agendamento> agendamentos = agendamentoRepo.buscarComFiltros(buscaFiltro, data_inicio, data_fim);

        if (estabelecimento != null && !estabelecimento.isBlank() && !"todos".equalsIgnoreCase(estabelecimento)) {
            String est = estabelecimento.strip().toUpperCase();
            agendamentos = agendamentos.stream()
                .filter(a -> est.equals(a.getEstabelecimento()))
                .toList();
        }

        model.addAttribute("anos", anos);
        model.addAttribute("agendamentos", agendamentos);
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
            .ofPattern("dd/MM/yyyy (EEEE)", new Locale("pt", "BR"));

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

    @GetMapping("/dashboard_exames")
    public String dashboardExames(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) String estabelecimento,
            @RequestParam(defaultValue = "30") int diasAviso,
            Model model) {

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

        long qtdVencidos = todos.stream().filter(f -> f.getAso() != null && f.getAso().isBefore(hoje)).count();
        long qtdAVencer  = todos.stream().filter(f -> f.getAso() != null && !f.getAso().isBefore(hoje) && f.getAso().isBefore(limite)).count();
        long qtdEmDia    = todos.stream().filter(f -> f.getAso() != null && !f.getAso().isBefore(limite)).count();
        long qtdSemAso   = todos.stream().filter(f -> f.getAso() == null).count();

        String statusFiltro = status != null ? status : "todos";
        List<Funcionario> exames = switch (statusFiltro) {
            case "vencidos"  -> todos.stream().filter(f -> f.getAso() != null && f.getAso().isBefore(hoje)).toList();
            case "a_vencer"  -> todos.stream().filter(f -> f.getAso() != null && !f.getAso().isBefore(hoje) && f.getAso().isBefore(limite)).toList();
            case "em_dia"    -> todos.stream().filter(f -> f.getAso() != null && !f.getAso().isBefore(limite)).toList();
            case "sem_aso"   -> todos.stream().filter(f -> f.getAso() == null).toList();
            default          -> todos;
        };

        exames = exames.stream()
            .sorted(Comparator.comparing(f -> f.getAso() != null ? f.getAso() : LocalDate.MAX))
            .toList();

        Map<Long, Long> diasAso = exames.stream()
            .filter(f -> f.getAso() != null)
            .collect(Collectors.toMap(
                Funcionario::getId,
                f -> ChronoUnit.DAYS.between(hoje, f.getAso())));

        model.addAttribute("exames",        exames);
        model.addAttribute("hoje",          hoje);
        model.addAttribute("diasAviso",     diasAviso);
        model.addAttribute("statusFiltro",  statusFiltro);
        model.addAttribute("busca",         busca);
        model.addAttribute("estabelecimento", estabelecimento);
        model.addAttribute("qtdTotal",      todos.size());
        model.addAttribute("qtdVencidos",   qtdVencidos);
        model.addAttribute("qtdAVencer",    qtdAVencer);
        model.addAttribute("qtdEmDia",      qtdEmDia);
        model.addAttribute("qtdSemAso",     qtdSemAso);
        model.addAttribute("diasAso",       diasAso);
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
        Map<String, Integer> contadorTipo = new LinkedHashMap<>();
        int[] contadorMes = new int[12];
        Set<Integer> anosDisponiveis = new TreeSet<>();

        for (Agendamento a : todos) {
            if (a.getDataClinico() == null) continue;
            int anoAg = a.getDataClinico().getYear();
            anosDisponiveis.add(anoAg);
            if (ano != null && ano != anoAg) continue;
            int mes = a.getDataClinico().getMonthValue() - 1;
            String tipo = a.getTipoExameDescricao();
            contadorMes[mes]++;
            contadorTipo.merge(tipo, 1, Integer::sum);
        }

        model.addAttribute("contadorTipoExame", contadorTipo);
        model.addAttribute("contadorExamesMes", contadorMes);
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

        // ── Exames por mês (últimos 12 meses) ───────────────────────────
        LocalDate inicioPeriodo = hoje.minusMonths(11).withDayOfMonth(1);
        List<Agendamento> agRecentes = agendamentoRepo.findAll().stream()
            .filter(a -> a.getDataClinico() != null && !a.getDataClinico().isBefore(inicioPeriodo))
            .toList();

        List<String>  labelesMeses = new ArrayList<>();
        List<Integer> examesPorMes = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM/yy", new java.util.Locale("pt", "BR"));
        for (int i = 11; i >= 0; i--) {
            LocalDate m = hoje.minusMonths(i).withDayOfMonth(1);
            labelesMeses.add(m.format(fmt));
            int anoM = m.getYear(), mesM = m.getMonthValue();
            examesPorMes.add((int) agRecentes.stream()
                .filter(a -> a.getDataClinico().getYear() == anoM && a.getDataClinico().getMonthValue() == mesM)
                .count());
        }

        // ── Distribuição por tipo ────────────────────────────────────────
        Map<String, Long> tiposDist = agRecentes.stream()
            .collect(Collectors.groupingBy(Agendamento::getTipoExameDescricao, Collectors.counting()));
        Map<String, Long> tiposOrdenados = tiposDist.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                                      (a, b) -> a, LinkedHashMap::new));

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
        model.addAttribute("labelesMeses",  labelesMeses);
        model.addAttribute("examesPorMes",  examesPorMes);
        model.addAttribute("tiposLabels",   new ArrayList<>(tiposOrdenados.keySet()));
        model.addAttribute("tiposTotal",    new ArrayList<>(tiposOrdenados.values()));
        model.addAttribute("resultados",    pagina_);
        model.addAttribute("totalItens",    totalItens);
        model.addAttribute("totalPaginas",  totalPaginas);
        model.addAttribute("paginaAtual",   paginaSeg);
        model.addAttribute("dias",          dias);
        return "indicadores";
    }
}
