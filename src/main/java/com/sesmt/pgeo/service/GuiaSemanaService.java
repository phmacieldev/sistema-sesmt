/*
 * Copyright (c) 2026 Pedro Henrique Maciel da Silva Faria. Todos os direitos reservados.
 */
package com.sesmt.pgeo.service;

import com.sesmt.pgeo.dto.GuiasSemanaResponseDto;
import com.sesmt.pgeo.dto.GuiasSemanaResponseDto.GuiaClinicoDto;
import com.sesmt.pgeo.dto.GuiasSemanaResponseDto.GuiaSangueDto;
import com.sesmt.pgeo.model.Agendamento;
import com.sesmt.pgeo.repository.AgendamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class GuiaSemanaService {

    private final AgendamentoRepository agendamentoRepo;

    public GuiasSemanaResponseDto montar() {
        LocalDate hoje = LocalDate.now();
        DateTimeFormatter dayFmt   = DateTimeFormatter.ofPattern("EEE dd/MM", Locale.of("pt", "BR"));
        DateTimeFormatter startFmt = DateTimeFormatter.ofPattern("dd/MM");
        DateTimeFormatter endFmt   = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        // Sangue: próximo dia útil (pula fim de semana)
        LocalDate proximoDiaUtil = proximoDiaUtil(hoje);

        // Clínico: semana seguinte (seg–sex)
        LocalDate proxSegunda = hoje.with(DayOfWeek.MONDAY).plusWeeks(1);
        LocalDate proxSexta   = proxSegunda.plusDays(4);

        var sangue = agendamentoRepo
            .findByDataSangueOrderByDataSangueAscHoraClinicoAsc(proximoDiaUtil)
            .stream()
            .map(a -> new GuiaSangueDto(
                a.getId(),
                a.getFuncionarioNome()  != null ? a.getFuncionarioNome()  : "",
                a.getFuncionarioSetor() != null ? a.getFuncionarioSetor() : "",
                a.getDataSangue().format(dayFmt),
                a.getHoraClinico()      != null ? a.getHoraClinico()      : "",
                a.getExamesSangue()     != null ? a.getExamesSangue()     : ""
            ))
            .toList();

        var clinico = agendamentoRepo
            .findByDataClinicoBetweenOrderByDataClinicoAsc(proxSegunda, proxSexta)
            .stream()
            .map(a -> new GuiaClinicoDto(
                a.getId(),
                a.getFuncionarioNome()  != null ? a.getFuncionarioNome()  : "",
                a.getFuncionarioSetor() != null ? a.getFuncionarioSetor() : "",
                a.getDataClinico().format(dayFmt),
                a.getHoraClinico()      != null ? a.getHoraClinico()      : "",
                a.getTipoExameDescricao()
            ))
            .toList();

        return new GuiasSemanaResponseDto(
            proximoDiaUtil.format(endFmt),
            proxSegunda.format(startFmt) + " a " + proxSexta.format(endFmt),
            sangue,
            clinico
        );
    }

    private LocalDate proximoDiaUtil(LocalDate ref) {
        LocalDate proximo = ref.plusDays(1);
        while (proximo.getDayOfWeek() == DayOfWeek.SATURDAY
            || proximo.getDayOfWeek() == DayOfWeek.SUNDAY) {
            proximo = proximo.plusDays(1);
        }
        return proximo;
    }
}
