package com.sesmt.pgeo.controller;

import com.sesmt.pgeo.model.Agendamento;
import com.sesmt.pgeo.model.Funcionario;
import com.sesmt.pgeo.model.MedicalLeave;
import com.sesmt.pgeo.repository.AgendamentoRepository;
import com.sesmt.pgeo.repository.FuncionarioRepository;
import com.sesmt.pgeo.repository.MedicalLeaveRepository;
import com.sesmt.pgeo.util.AppConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final AgendamentoRepository agendamentoRepo;
    private final FuncionarioRepository funcionarioRepo;
    private final MedicalLeaveRepository medicalLeaveRepo;

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

    @GetMapping("/dashboard_dados")
    public String dashboardDados(
            @RequestParam(required = false) Integer mes,
            @RequestParam(required = false) Integer ano,
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data_inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data_fim,
            @RequestParam(required = false) String estabelecimento,
            @RequestParam(required = false) String aplicar_filtro,
            Model model) {

        List<Agendamento> agendamentos;

        if ("true".equals(aplicar_filtro) && mes != null && ano != null
                && data_inicio == null && data_fim == null) {
            agendamentos = agendamentoRepo.findByMesEAno(mes, ano);
            if (busca != null && !busca.isBlank()) {
                String b = busca.strip().toLowerCase();
                agendamentos = agendamentos.stream()
                    .filter(a -> a.getFuncionarioNome() != null &&
                                 a.getFuncionarioNome().toLowerCase().contains(b))
                    .toList();
            }
        } else {
            String buscaFiltro = (busca != null && !busca.isBlank()) ? busca.strip() : null;
            agendamentos = agendamentoRepo.buscarComFiltros(buscaFiltro, data_inicio, data_fim);
        }

        if (estabelecimento != null && !estabelecimento.isBlank() && !"todos".equalsIgnoreCase(estabelecimento)) {
            String est = estabelecimento.strip().toUpperCase();
            agendamentos = agendamentos.stream()
                .filter(a -> est.equals(a.getEstabelecimento()))
                .toList();
        }

        model.addAttribute("agendamentos", agendamentos);
        return "_tabela_agendamentos :: tbody";
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
    public String dashboardExames(Model model) {
        List<Funcionario> funcionarios = funcionarioRepo.findByAtivoTrue()
            .stream()
            .sorted(Comparator.comparing(
                f -> f.getAso() != null ? f.getAso() : LocalDate.MIN))
            .toList();
        model.addAttribute("exames", funcionarios);
        model.addAttribute("hoje", LocalDate.now());
        model.addAttribute("limite30", LocalDate.now().plusDays(30));
        return "dashboard_exames";
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

    @GetMapping("/indicadores")
    public String indicadores(Model model) {
        LocalDate limite = LocalDate.now().minusDays(60);
        List<MedicalLeave> recentes = medicalLeaveRepo.findRecentes(limite);

        Map<String, Integer> totais = recentes.stream()
            .collect(Collectors.groupingBy(
                ml -> ml.getFuncionario().getNome(),
                Collectors.summingInt(MedicalLeave::getDiasAfastamento)));

        model.addAttribute("resultados", totais.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .toList());
        return "indicadores_atestados";
    }
}
