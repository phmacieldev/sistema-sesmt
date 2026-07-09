/*
 * Copyright (c) 2026 Pedro Henrique Maciel da Silva Faria. Todos os direitos reservados.
 * Desenvolvido de forma independente como projeto de portfólio.
 * Autorizado apenas para uso interno homologado.
 */
package com.sesmt.pgeo.service;

import com.sesmt.pgeo.dto.GuiasSemanaResponseDto;
import com.sesmt.pgeo.model.Agendamento;
import com.sesmt.pgeo.repository.AgendamentoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuiaSemanaServiceTest {

    @Mock AgendamentoRepository agendamentoRepo;

    @InjectMocks GuiaSemanaService service;

    // Referências fixas de 2026: 06/07 = segunda, 10/07 = sexta, 12/07 = domingo

    @Test
    void diaUtilComum_sangueEhAmanha_clinicoEhSemanaSeguinte() {
        LocalDate quarta = LocalDate.of(2026, 7, 8);
        when(agendamentoRepo.findByDataSangueOrderByDataSangueAscHoraClinicoAsc(any())).thenReturn(List.of());
        when(agendamentoRepo.findByDataClinicoBetweenOrderByDataClinicoAsc(any(), any())).thenReturn(List.of());

        service.montar(quarta);

        // Sangue: amanhã (quinta 09/07)
        verify(agendamentoRepo).findByDataSangueOrderByDataSangueAscHoraClinicoAsc(LocalDate.of(2026, 7, 9));
        // Clínico: semana seguinte (seg 13/07 a sex 17/07)
        verify(agendamentoRepo).findByDataClinicoBetweenOrderByDataClinicoAsc(
            LocalDate.of(2026, 7, 13), LocalDate.of(2026, 7, 17));
    }

    @Test
    void sexta_sanguePulaFimDeSemana_paraSegunda() {
        LocalDate sexta = LocalDate.of(2026, 7, 10);
        when(agendamentoRepo.findByDataSangueOrderByDataSangueAscHoraClinicoAsc(any())).thenReturn(List.of());
        when(agendamentoRepo.findByDataClinicoBetweenOrderByDataClinicoAsc(any(), any())).thenReturn(List.of());

        service.montar(sexta);

        // Próximo dia útil após sexta = segunda 13/07
        verify(agendamentoRepo).findByDataSangueOrderByDataSangueAscHoraClinicoAsc(LocalDate.of(2026, 7, 13));
        // Clínico: semana seguinte 13/07–17/07
        verify(agendamentoRepo).findByDataClinicoBetweenOrderByDataClinicoAsc(
            LocalDate.of(2026, 7, 13), LocalDate.of(2026, 7, 17));
    }

    @Test
    void domingo_clinicoNaoPulaUmaSemana() {
        // Regressão: with(MONDAY).plusWeeks(1) no domingo retornava a segunda de 8 dias depois
        LocalDate domingo = LocalDate.of(2026, 7, 12);
        when(agendamentoRepo.findByDataSangueOrderByDataSangueAscHoraClinicoAsc(any())).thenReturn(List.of());
        when(agendamentoRepo.findByDataClinicoBetweenOrderByDataClinicoAsc(any(), any())).thenReturn(List.of());

        service.montar(domingo);

        // Semana seguinte ao domingo 12/07 começa na segunda 13/07 — não 20/07
        verify(agendamentoRepo).findByDataClinicoBetweenOrderByDataClinicoAsc(
            LocalDate.of(2026, 7, 13), LocalDate.of(2026, 7, 17));
    }

    @Test
    void montar_preencheDtoComPeriodosEListas() {
        LocalDate quarta = LocalDate.of(2026, 7, 8);

        Agendamento sangue = new Agendamento();
        sangue.setId(1L);
        sangue.setFuncionarioNome("Ana");
        sangue.setDataSangue(LocalDate.of(2026, 7, 9));

        Agendamento clinico = new Agendamento();
        clinico.setId(2L);
        clinico.setFuncionarioNome("Beto");
        clinico.setDataClinico(LocalDate.of(2026, 7, 14));

        when(agendamentoRepo.findByDataSangueOrderByDataSangueAscHoraClinicoAsc(any()))
            .thenReturn(List.of(sangue));
        when(agendamentoRepo.findByDataClinicoBetweenOrderByDataClinicoAsc(any(), any()))
            .thenReturn(List.of(clinico));

        GuiasSemanaResponseDto dto = service.montar(quarta);

        assertThat(dto.periodoSangue()).isEqualTo("09/07/2026");
        assertThat(dto.periodoClinico()).isEqualTo("13/07 a 17/07/2026");
        assertThat(dto.sangue()).hasSize(1);
        assertThat(dto.sangue().get(0).nome()).isEqualTo("Ana");
        assertThat(dto.clinico()).hasSize(1);
        assertThat(dto.clinico().get(0).nome()).isEqualTo("Beto");
    }
}
