/*
 * Copyright (c) 2026 Pedro Henrique Maciel da Silva Faria. Todos os direitos reservados.
 * Desenvolvido de forma independente como projeto de portfólio.
 * Autorizado apenas para uso interno homologado.
 */
package com.sesmt.pgeo.service;

import com.sesmt.pgeo.audit.AuditService;
import com.sesmt.pgeo.exception.RegraDeNegocioException;
import com.sesmt.pgeo.model.Agendamento;
import com.sesmt.pgeo.model.Funcionario;
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

import java.time.LocalDate;
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

    // ── Recebido: marca, guarda backup e atualiza validade NR-7 ─────────

    @Test
    void marcarRecebido_guardaDataAnteriorEAtualizaAsoMais12Meses() {
        Funcionario func = new Funcionario();
        func.setAso(LocalDate.of(2025, 3, 10));

        Agendamento ag = new Agendamento();
        ag.setId(1L);
        ag.setFuncionarioNome("Ana");
        ag.setFuncionario(func);
        ag.setDataClinico(LocalDate.of(2026, 7, 1));

        when(agendamentoRepo.findById(1L)).thenReturn(Optional.of(ag));

        asoService.atualizarStatusAso(1L, "recebido", true, "usuario-teste");

        assertThat(ag.isAsoRecebido()).isTrue();
        assertThat(ag.getDataAsoAnterior()).isEqualTo(LocalDate.of(2025, 3, 10));
        // Validade NR-7: data do exame + 12 meses
        assertThat(func.getAso()).isEqualTo(LocalDate.of(2027, 7, 1));
        verify(funcionarioRepo).save(func);
        verify(notificacaoService).broadcastAsoStatus(ag, "usuario-teste");
    }

    @Test
    void marcarRecebido_jaRecebido_naoReaplicaNemSobrescreveBackup() {
        Funcionario func = new Funcionario();
        func.setAso(LocalDate.of(2027, 7, 1)); // já atualizado pela 1ª marcação

        Agendamento ag = new Agendamento();
        ag.setId(1L);
        ag.setFuncionario(func);
        ag.setAsoRecebido(true);
        ag.setDataAsoAnterior(LocalDate.of(2025, 3, 10));
        ag.setDataClinico(LocalDate.of(2026, 7, 1));

        when(agendamentoRepo.findById(1L)).thenReturn(Optional.of(ag));

        boolean resultado = asoService.atualizarStatusAso(1L, "recebido", true, "usuario-teste");

        assertThat(resultado).isTrue();
        // Backup preservado — a segunda marcação não sobrescreve com o valor já atualizado
        assertThat(ag.getDataAsoAnterior()).isEqualTo(LocalDate.of(2025, 3, 10));
        verify(funcionarioRepo, never()).save(any());
        verify(agendamentoRepo, never()).save(any());
    }

    // ── Reversão ────────────────────────────────────────────────────────

    @Test
    void reverterRecebido_restauraDataAnteriorDoFuncionario() {
        Funcionario func = new Funcionario();
        func.setAso(LocalDate.of(2027, 7, 1));

        Agendamento ag = new Agendamento();
        ag.setId(1L);
        ag.setFuncionarioNome("Ana");
        ag.setFuncionario(func);
        ag.setAsoRecebido(true);
        ag.setDataAsoAnterior(LocalDate.of(2025, 3, 10));

        when(agendamentoRepo.findById(1L)).thenReturn(Optional.of(ag));

        asoService.atualizarStatusAso(1L, "recebido", false, "usuario-teste");

        assertThat(ag.isAsoRecebido()).isFalse();
        assertThat(ag.getDataAsoAnterior()).isNull();
        assertThat(func.getAso()).isEqualTo(LocalDate.of(2025, 3, 10));
        verify(funcionarioRepo).save(func);
    }

    @Test
    void reverterRecebido_semFuncionario_lancaExcecao() {
        Agendamento ag = new Agendamento();
        ag.setId(1L);
        ag.setAsoRecebido(true);
        // sem FK de funcionário e sem matrícula

        when(agendamentoRepo.findById(1L)).thenReturn(Optional.of(ag));

        assertThatThrownBy(() ->
            asoService.atualizarStatusAso(1L, "recebido", false, "usuario-teste"))
            .isInstanceOf(RegraDeNegocioException.class)
            .hasMessageContaining("sem vínculo");

        // Nada deve ter sido alterado
        assertThat(ag.isAsoRecebido()).isTrue();
        verify(agendamentoRepo, never()).save(any());
    }
}
