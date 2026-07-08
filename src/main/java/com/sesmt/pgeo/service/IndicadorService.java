/*
 * Copyright (c) 2026 Pedro Henrique Maciel da Silva Faria. Todos os direitos reservados.
 */
package com.sesmt.pgeo.service;

import com.sesmt.pgeo.model.Agendamento;
import com.sesmt.pgeo.model.MedicalLeave;
import com.sesmt.pgeo.repository.AgendamentoRepository;
import com.sesmt.pgeo.repository.FuncionarioRepository;
import com.sesmt.pgeo.repository.MedicalLeaveRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IndicadorService {

    private final FuncionarioRepository  funcionarioRepo;
    private final AgendamentoRepository  agendamentoRepo;
    private final MedicalLeaveRepository medicalLeaveRepo;

    public record AsoStatus(long vencidos, long aVencer, long emDia, long semAso, long totalAtivos) {}

    public AsoStatus calcularStatusAso() {
        LocalDate hoje     = LocalDate.now();
        LocalDate limite30 = hoje.plusDays(30);
        return new AsoStatus(
            funcionarioRepo.countAsoVencidos(hoje),
            funcionarioRepo.countAsoAVencer(hoje, limite30),
            funcionarioRepo.countAsoEmDia(limite30),
            funcionarioRepo.countSemAso(),
            funcionarioRepo.countAtivos()
        );
    }

    public record AgendamentoStatus(long agendados, long emDia, long atrasados) {}

    public AgendamentoStatus calcularStatusAgendamentos() {
        LocalDate hoje = LocalDate.now();
        return new AgendamentoStatus(
            agendamentoRepo.countAgendados(hoje),
            agendamentoRepo.countEmDia(hoje),
            agendamentoRepo.countAtrasados(hoje)
        );
    }

    public record AtestadosPorMes(List<String> labels, List<Integer> valores) {}

    public AtestadosPorMes calcularAtestadosPorMes() {
        LocalDate hoje = LocalDate.now();
        LocalDate inicio = hoje.minusMonths(11).withDayOfMonth(1);
        List<MedicalLeave> recentes = medicalLeaveRepo.findDesde(inicio);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM/yy", new Locale("pt", "BR"));

        List<String>  labels  = new ArrayList<>();
        List<Integer> valores = new ArrayList<>();
        for (int i = 11; i >= 0; i--) {
            LocalDate m = hoje.minusMonths(i).withDayOfMonth(1);
            labels.add(m.format(fmt));
            int anoM = m.getYear(), mesM = m.getMonthValue();
            valores.add((int) recentes.stream()
                .filter(ml -> ml.getDataAfastamento().getYear() == anoM
                           && ml.getDataAfastamento().getMonthValue() == mesM)
                .count());
        }
        return new AtestadosPorMes(labels, valores);
    }

    public record RankingEntry(String nome, int dias) {}

    public record RankingAtestados(List<RankingEntry> todos, int totalItens) {}

    public RankingAtestados calcularRanking(int dias) {
        LocalDate limite = LocalDate.now().minusDays(dias);
        List<RankingEntry> ranking = medicalLeaveRepo.findRecentes(limite)
            .stream()
            .collect(Collectors.groupingBy(
                ml -> ml.getFuncionario().getNome(),
                Collectors.summingInt(MedicalLeave::getDiasAfastamento)))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .map(e -> new RankingEntry(e.getKey(), e.getValue()))
            .toList();
        return new RankingAtestados(ranking, ranking.size());
    }

    public record EstatisticasExames(
        Map<String, Integer> porTipo, int[] porMes,
        Map<String, Integer> porSetor, Map<String, Integer> porEstabelecimento,
        int total, Set<Integer> anosDisponiveis
    ) {}

    public EstatisticasExames calcularEstatisticas(Integer ano) {
        List<Integer> anosDisp = agendamentoRepo.findAnosDisponiveis();
        List<Agendamento> filtrados = agendamentoRepo.findByAnoOptional(ano);

        Map<String, Integer> porTipo  = new LinkedHashMap<>();
        Map<String, Integer> porSetor = new LinkedHashMap<>();
        Map<String, Integer> porEstab = new LinkedHashMap<>();
        int[] porMes = new int[12];

        for (Agendamento a : filtrados) {
            porMes[a.getDataClinico().getMonthValue() - 1]++;
            porTipo.merge(a.getTipoExameDescricao(), 1, Integer::sum);

            String setor = a.getFuncionarioSetor() != null && !a.getFuncionarioSetor().isBlank()
                ? a.getFuncionarioSetor() : "Não informado";
            porSetor.merge(setor, 1, Integer::sum);

            String estab = "Não informado";
            try {
                if (a.getFuncionario() != null && a.getFuncionario().getEstabelecimentoEfetivo() != null
                        && !a.getFuncionario().getEstabelecimentoEfetivo().isBlank())
                    estab = a.getFuncionario().getEstabelecimentoEfetivo();
            } catch (Exception ignored) {}
            porEstab.merge(estab, 1, Integer::sum);
        }

        // Top 10 setores
        Map<String, Integer> topSetores = porSetor.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(10)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                     (a, b) -> a, LinkedHashMap::new));

        Map<String, Integer> topEstab = porEstab.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                     (a, b) -> a, LinkedHashMap::new));

        return new EstatisticasExames(porTipo, porMes, topSetores, topEstab,
            filtrados.size(), new TreeSet<>(anosDisp));
    }
}
