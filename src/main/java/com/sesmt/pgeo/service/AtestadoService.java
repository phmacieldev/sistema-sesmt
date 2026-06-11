/*
 * Copyright (c) 2026 Pedro Henrique Maciel da Silva Faria. Todos os direitos reservados.
 * Desenvolvido de forma independente como projeto de portfólio.
 * Autorizado apenas para uso interno homologado.
 */
package com.sesmt.pgeo.service;

import com.sesmt.pgeo.model.MedicalLeave;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AtestadoService {

    /** Encontra a terça-feira que inicia a semana do dia fornecido. */
    public LocalDate semanaInicio(LocalDate referencia) {
        int daysBack = (referencia.getDayOfWeek().getValue() - DayOfWeek.TUESDAY.getValue() + 7) % 7;
        return referencia.minusDays(daysBack);
    }

    public LocalDate semanaFim(LocalDate semanaInicio) {
        return semanaInicio.plusDays(6);
    }

    /** Agrupa dias de afastamento por funcionário nos últimos 60 dias. Marca >= 15 dias como risco INSS. */
    @Data
    public static class ResumoFuncionario {
        private final Long funcionarioId;
        private final String nome;
        private final String setor;
        private final int totalDias;
        private final int totalAtestados;
        private final List<MedicalLeave> atestados;
        public boolean isRiscoInss() { return totalDias >= 15; }
    }

    public List<ResumoFuncionario> resumoPor60Dias(List<MedicalLeave> atestados) {
        return atestados.stream()
            .collect(Collectors.groupingBy(ml -> ml.getFuncionario().getId()))
            .values().stream()
            .map(lista -> {
                MedicalLeave primeiro = lista.get(0);
                int totalDias = lista.stream().mapToInt(MedicalLeave::getDiasAfastamento).sum();
                return new ResumoFuncionario(
                    primeiro.getFuncionario().getId(),
                    primeiro.getFuncionario().getNome(),
                    primeiro.getFuncionario().getSetor(),
                    totalDias,
                    lista.size(),
                    lista
                );
            })
            .sorted(Comparator.comparingInt(ResumoFuncionario::getTotalDias).reversed())
            .collect(Collectors.toList());
    }

    /** Totais por setor: Map<setor, totalDias> */
    public Map<String, Integer> totalPorSetor(List<MedicalLeave> lista) {
        return lista.stream().collect(Collectors.groupingBy(
            ml -> ml.getFuncionario().getSetor() != null ? ml.getFuncionario().getSetor() : "Não informado",
            TreeMap::new,
            Collectors.summingInt(MedicalLeave::getDiasAfastamento)
        ));
    }

    /** Totais por tipo: Map<descricao, count> */
    public Map<String, Long> totalPorTipo(List<MedicalLeave> lista) {
        return lista.stream().collect(Collectors.groupingBy(
            ml -> ml.getTipo() != null ? ml.getTipo().getDescricao() : "Não Informado",
            TreeMap::new,
            Collectors.counting()
        ));
    }
}
