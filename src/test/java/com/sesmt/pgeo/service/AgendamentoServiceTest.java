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
import com.sesmt.pgeo.model.enums.StatusFuncionario;
import com.sesmt.pgeo.model.enums.TipoExame;
import com.sesmt.pgeo.repository.AgendamentoRepository;
import com.sesmt.pgeo.repository.FuncionarioRepository;
import com.sesmt.pgeo.websocket.NotificacaoService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AgendamentoServiceTest {

    @Mock AgendamentoRepository agendamentoRepo;
    @Mock FuncionarioRepository funcionarioRepo;
    @Mock AuditService auditService;
    @Mock NotificacaoService notificacaoService;

    @InjectMocks AgendamentoService agendamentoService;

    @BeforeEach
    void configurarSeguranca() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("usuario-teste");
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    // ── sangunePodeAgendar ────────────────────────────────────────────

    @Test
    void sangunePodeAgendar_retornaTrue_quandoAbaixoDoLimite() {
        LocalDate data = proximoDiaUtil(DayOfWeek.WEDNESDAY);
        when(agendamentoRepo.countByDataSangue(data)).thenReturn(3L);

        assertThat(agendamentoService.sangunePodeAgendar(data)).isTrue();
    }

    @Test
    void sangunePodeAgendar_retornaFalse_quandoLimiteAtingido() {
        LocalDate data = proximoDiaUtil(DayOfWeek.WEDNESDAY);
        when(agendamentoRepo.countByDataSangue(data)).thenReturn(5L);

        assertThat(agendamentoService.sangunePodeAgendar(data)).isFalse();
    }

    @Test
    void sangunePodeAgendar_retornaTrue_quandoDataNula() {
        assertThat(agendamentoService.sangunePodeAgendar(null)).isTrue();
    }

    // ── criar ─────────────────────────────────────────────────────────

    @Test
    void criar_retornaAgendamento_quandoDadosValidos() {
        LocalDate segunda = proximoDiaUtil(DayOfWeek.MONDAY);
        Funcionario func = funcionarioAtivo(1L, "Ana Lima");

        when(agendamentoRepo.countByDataSangue(any())).thenReturn(0L);
        when(agendamentoRepo.findByMatriculaEAno(any(), anyInt())).thenReturn(Optional.empty());
        when(funcionarioRepo.findByMatricula("99999")).thenReturn(Optional.of(func));
        when(agendamentoRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Agendamento resultado = agendamentoService.criar(
            "99999", "Ana Lima", "TI", "Analista",
            TipoExame.PERIODICO, segunda, "09:00", segunda, null, null);

        assertThat(resultado.getTipoExame()).isEqualTo(TipoExame.PERIODICO);
        verify(agendamentoRepo).save(any(Agendamento.class));
        verify(auditService).registrarCriacao(eq("Agendamento"), any(), any());
    }

    @Test
    void criar_lancaExcecao_quandoDataClinicoSabado() {
        LocalDate sabado = proximoDiaUtil(DayOfWeek.SATURDAY);

        assertThatThrownBy(() -> agendamentoService.criar(
            "12345", "João", "TI", "Dev",
            TipoExame.PERIODICO, sabado, "09:00", null, null, null))
            .isInstanceOf(RegraDeNegocioException.class)
            .hasMessageContaining("fim de semana");
    }

    @Test
    void criar_lancaExcecao_quandoDataClinicoDomingo() {
        LocalDate domingo = proximoDiaUtil(DayOfWeek.SUNDAY);

        assertThatThrownBy(() -> agendamentoService.criar(
            "12345", "João", "TI", "Dev",
            TipoExame.PERIODICO, domingo, "09:00", null, null, null))
            .isInstanceOf(RegraDeNegocioException.class)
            .hasMessageContaining("fim de semana");
    }

    @Test
    void criar_lancaExcecao_quandoLimiteDeSangueAtingido() {
        LocalDate terca = proximoDiaUtil(DayOfWeek.TUESDAY);
        when(agendamentoRepo.countByDataSangue(terca)).thenReturn(5L);

        assertThatThrownBy(() -> agendamentoService.criar(
            "12345", "Maria", "RH", "Analista",
            TipoExame.PERIODICO, terca, "09:00", terca, null, null))
            .isInstanceOf(RegraDeNegocioException.class)
            .hasMessageContaining("Limite");
    }

    @Test
    void criar_lancaExcecao_quandoDuplicadoNoAno() {
        LocalDate quarta = proximoDiaUtil(DayOfWeek.WEDNESDAY);
        Agendamento existente = new Agendamento();
        existente.setId(42L);
        existente.setFuncionarioNome("Carlos");
        existente.setDataClinico(quarta);
        existente.setHoraClinico("10:00");

        when(agendamentoRepo.countByDataSangue(any())).thenReturn(0L);
        when(agendamentoRepo.findByMatriculaEAno(eq("55555"), anyInt()))
            .thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> agendamentoService.criar(
            "55555", "Carlos", "Eng", "Engenheiro",
            TipoExame.PERIODICO, quarta, "09:00", null, null, null))
            .isInstanceOf(RegraDeNegocioException.class)
            .hasMessageContaining("DUPLICADO");
    }

    @Test
    void criar_lancaExcecao_quandoMatriculaVaziaEExameNaoAdmissional() {
        LocalDate segunda = proximoDiaUtil(DayOfWeek.MONDAY);

        assertThatThrownBy(() -> agendamentoService.criar(
            "", "Sem Matrícula", "RH", "Aux",
            TipoExame.PERIODICO, segunda, "08:00", null, null, null))
            .isInstanceOf(RegraDeNegocioException.class)
            .hasMessageContaining("Matrícula é obrigatória");
    }

    // ── editar ────────────────────────────────────────────────────────

    @Test
    void editar_atualizaDadosDoFuncionarioVinculado_quandoDivergem() {
        Funcionario func = funcionarioAtivo(1L, "Nome Antigo");
        func.setSetor("Setor Antigo");
        func.setFuncao("Funcao Antiga");

        Agendamento ag = new Agendamento();
        ag.setId(20L);
        ag.setFuncionario(func);
        ag.setDataClinico(proximoDiaUtil(DayOfWeek.MONDAY));
        ag.setHoraClinico("08:00");

        when(agendamentoRepo.findById(20L)).thenReturn(Optional.of(ag));
        when(agendamentoRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        agendamentoService.editar(20L, "Nome Novo", "Setor Novo", "Funcao Nova",
            TipoExame.PERIODICO, proximoDiaUtil(DayOfWeek.TUESDAY), null, "09:00", null, null);

        // Antes da correção, esses campos eram setados só no cache do Agendamento
        // e descartados pelo @PreUpdate — a mudança real precisa refletir no Funcionario.
        assertThat(func.getNome()).isEqualTo("Nome Novo");
        assertThat(func.getSetor()).isEqualTo("Setor Novo");
        assertThat(func.getFuncao()).isEqualTo("Funcao Nova");
        verify(funcionarioRepo).save(func);
    }

    @Test
    void editar_naoSalvaFuncionario_quandoDadosNaoMudam() {
        Funcionario func = funcionarioAtivo(1L, "Nome Igual");
        func.setSetor("Setor Igual");
        func.setFuncao("Funcao Igual");

        Agendamento ag = new Agendamento();
        ag.setId(21L);
        ag.setFuncionario(func);
        ag.setDataClinico(proximoDiaUtil(DayOfWeek.MONDAY));
        ag.setHoraClinico("08:00");

        when(agendamentoRepo.findById(21L)).thenReturn(Optional.of(ag));
        when(agendamentoRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        agendamentoService.editar(21L, "Nome Igual", "Setor Igual", "Funcao Igual",
            TipoExame.PERIODICO, proximoDiaUtil(DayOfWeek.TUESDAY), null, "09:00", null, null);

        verify(funcionarioRepo, never()).save(any());
    }

    // ── excluir ───────────────────────────────────────────────────────

    @Test
    void excluir_chamaDeletEAuditoria() {
        Agendamento ag = new Agendamento();
        ag.setId(10L);
        ag.setFuncionarioNome("Pedro");
        ag.setDataClinico(proximoDiaUtil(DayOfWeek.MONDAY));
        ag.setHoraClinico("08:00");

        when(agendamentoRepo.findById(10L)).thenReturn(Optional.of(ag));

        agendamentoService.excluir(10L);

        verify(agendamentoRepo).delete(ag);
        verify(auditService).registrarExclusao(eq("Agendamento"), eq(10L), any(), any());
        verify(notificacaoService).broadcastExclusao(eq(10L), eq("Pedro"), any());
    }

    // ── mover ─────────────────────────────────────────────────────────

    @Test
    void mover_atualizaDataEHora() {
        LocalDate novaData = proximoDiaUtil(DayOfWeek.THURSDAY);
        Agendamento ag = new Agendamento();
        ag.setId(5L);
        ag.setFuncionarioNome("Lucas");
        ag.setDataClinico(proximoDiaUtil(DayOfWeek.MONDAY));
        ag.setHoraClinico("08:00");

        when(agendamentoRepo.findById(5L)).thenReturn(Optional.of(ag));
        when(agendamentoRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Agendamento resultado = agendamentoService.mover(5L, novaData, "14:00");

        assertThat(resultado.getDataClinico()).isEqualTo(novaData);
        assertThat(resultado.getHoraClinico()).isEqualTo("14:00");
    }

    @Test
    void mover_lancaExcecao_quandoNovaDataFimDeSemana() {
        LocalDate sabado = proximoDiaUtil(DayOfWeek.SATURDAY);
        Agendamento ag = new Agendamento();
        ag.setId(7L);
        ag.setFuncionarioNome("Teste");

        when(agendamentoRepo.findById(7L)).thenReturn(Optional.of(ag));

        assertThatThrownBy(() -> agendamentoService.mover(7L, sabado, "09:00"))
            .isInstanceOf(RegraDeNegocioException.class)
            .hasMessageContaining("fim de semana");
    }

    // ── helpers ───────────────────────────────────────────────────────

    private LocalDate proximoDiaUtil(DayOfWeek dia) {
        LocalDate data = LocalDate.now().with(dia);
        if (!data.isAfter(LocalDate.now())) data = data.plusWeeks(1);
        return data;
    }

    private Funcionario funcionarioAtivo(Long id, String nome) {
        Funcionario f = new Funcionario();
        f.setId(id);
        f.setNome(nome);
        f.setStatus(StatusFuncionario.ATIVO);
        f.setAtivo(true);
        return f;
    }
}
