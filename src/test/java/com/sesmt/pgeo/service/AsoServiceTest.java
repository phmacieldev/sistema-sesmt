/*
 * Copyright (c) 2026 Pedro Henrique Maciel da Silva Faria. Todos os direitos reservados.
 * Desenvolvido de forma independente como projeto de portfólio.
 * Autorizado apenas para uso interno homologado.
 */
package com.sesmt.pgeo.service;

import com.sesmt.pgeo.audit.AuditService;
import com.sesmt.pgeo.exception.RegraDeNegocioException;
import com.sesmt.pgeo.model.Agendamento;
import com.sesmt.pgeo.repository.AgendamentoRepository;
import com.sesmt.pgeo.repository.FuncionarioRepository;
import com.sesmt.pgeo.websocket.NotificacaoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AsoServiceTest {

    @Mock AgendamentoRepository agendamentoRepo;
    @Mock FuncionarioRepository funcionarioRepo;
    @Mock AuditService auditService;
    @Mock NotificacaoService notificacaoService;

    @InjectMocks AsoService asoService;

    @Test
    void atualizarStatusAso_lancaExcecao_quandoCampoInvalido() {
        assertThatThrownBy(() ->
            asoService.atualizarStatusAso(1L, "campo_que_nao_existe", true, "usuario-teste"))
            .isInstanceOf(RegraDeNegocioException.class)
            .hasMessageContaining("campo_que_nao_existe");

        verifyNoInteractions(agendamentoRepo);
    }

    @Test
    void atualizarStatusAso_marcaEnviado_quandoCampoValido() {
        Agendamento ag = new Agendamento();
        ag.setId(1L);
        ag.setFuncionarioNome("Ana");

        when(agendamentoRepo.findById(1L)).thenReturn(Optional.of(ag));

        boolean resultado = asoService.atualizarStatusAso(1L, "enviado", true, "usuario-teste");

        assertThat(resultado).isTrue();
        assertThat(ag.isAsoEnviado()).isTrue();
        verify(agendamentoRepo).save(ag);
        verify(auditService).registrar(eq("ASO_ENVIADO"), eq("Agendamento"), eq(1L), any());
    }
}
