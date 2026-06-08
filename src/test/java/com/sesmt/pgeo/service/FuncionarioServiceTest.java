package com.sesmt.pgeo.service;

import com.sesmt.pgeo.audit.AuditService;
import com.sesmt.pgeo.exception.RegraDeNegocioException;
import com.sesmt.pgeo.model.Funcionario;
import com.sesmt.pgeo.model.HistoricoCargo;
import com.sesmt.pgeo.model.enums.StatusFuncionario;
import com.sesmt.pgeo.repository.FuncionarioRepository;
import com.sesmt.pgeo.repository.HistoricoCargoRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FuncionarioServiceTest {

    @Mock FuncionarioRepository funcionarioRepo;
    @Mock HistoricoCargoRepository historicoRepo;
    @Mock AuditService auditService;

    @InjectMocks FuncionarioService funcionarioService;

    @Captor ArgumentCaptor<HistoricoCargo> historicoCaptor;
    @Captor ArgumentCaptor<Funcionario> funcionarioCaptor;

    @BeforeEach
    void configurarSeguranca() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("admin");
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    // ── alterarCargo ──────────────────────────────────────────────────

    @Test
    void alterarCargo_registraHistoricoComDadosAnteriores() {
        Funcionario func = funcionarioAtivo(10L, "João", "RH", "Assistente");
        when(funcionarioRepo.findById(10L)).thenReturn(Optional.of(func));
        when(funcionarioRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        funcionarioService.alterarCargo(10L, "TI", "Analista", false, "TRANSFERENCIA");

        verify(historicoRepo).save(historicoCaptor.capture());
        HistoricoCargo hist = historicoCaptor.getValue();
        assertThat(hist.getCargoAnterior()).isEqualTo("Assistente");
        assertThat(hist.getSetorAnterior()).isEqualTo("RH");
        assertThat(hist.getCargoNovo()).isEqualTo("Analista");
        assertThat(hist.getSetorNovo()).isEqualTo("TI");
        assertThat(hist.getMotivo()).isEqualTo("TRANSFERENCIA");
    }

    @Test
    void alterarCargo_atualizaFuncionarioComNovosValores() {
        Funcionario func = funcionarioAtivo(10L, "João", "RH", "Assistente");
        when(funcionarioRepo.findById(10L)).thenReturn(Optional.of(func));
        when(funcionarioRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        funcionarioService.alterarCargo(10L, "TI", "Analista", false, "TRANSFERENCIA");

        verify(funcionarioRepo).save(funcionarioCaptor.capture());
        Funcionario salvo = funcionarioCaptor.getValue();
        assertThat(salvo.getFuncao()).isEqualTo("Analista");
        assertThat(salvo.getSetor()).isEqualTo("TI");
        assertThat(salvo.isExigeSangue()).isFalse();
    }

    @Test
    void alterarCargo_registraAuditoria() {
        Funcionario func = funcionarioAtivo(10L, "João", "RH", "Assistente");
        when(funcionarioRepo.findById(10L)).thenReturn(Optional.of(func));
        when(funcionarioRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        funcionarioService.alterarCargo(10L, "TI", "Analista", false, "MUDANCA_DE_RISCO");

        verify(auditService).registrar(eq("ALTERACAO_CARGO"), eq("Funcionario"), eq(10L), any());
    }

    // ── criarPreAdmissional ───────────────────────────────────────────

    @Test
    void criarPreAdmissional_lancaExcecao_quandoNomeVazio() {
        assertThatThrownBy(() -> funcionarioService.criarPreAdmissional("", "TI", "Dev", true))
            .isInstanceOf(RegraDeNegocioException.class)
            .hasMessageContaining("Nome é obrigatório");
    }

    @Test
    void criarPreAdmissional_lancaExcecao_quandoNomeNulo() {
        assertThatThrownBy(() -> funcionarioService.criarPreAdmissional(null, "TI", "Dev", true))
            .isInstanceOf(RegraDeNegocioException.class)
            .hasMessageContaining("Nome é obrigatório");
    }

    @Test
    void criarPreAdmissional_criaCom_statusPreAdmissional() {
        when(funcionarioRepo.count()).thenReturn(5L);
        when(funcionarioRepo.save(any())).thenAnswer(inv -> {
            Funcionario f = inv.getArgument(0);
            f.setId(99L);
            return f;
        });

        Funcionario resultado = funcionarioService.criarPreAdmissional(
            "Novo Colaborador", "Produção", "Operador", true);

        assertThat(resultado.getStatus()).isEqualTo(StatusFuncionario.PRE_ADMISSIONAL);
        assertThat(resultado.getNome()).isEqualTo("Novo Colaborador");
        assertThat(resultado.isAtivo()).isTrue();
        assertThat(resultado.getMatricula()).startsWith("ADM");
    }

    // ── efetivarAdmissional ───────────────────────────────────────────

    @Test
    void efetivarAdmissional_lancaExcecao_quandoNaoEhPreAdmissional() {
        Funcionario func = funcionarioAtivo(20L, "Maria", "RH", "Gerente");
        when(funcionarioRepo.findById(20L)).thenReturn(Optional.of(func));

        assertThatThrownBy(() -> funcionarioService.efetivarAdmissional(20L, "MAT001"))
            .isInstanceOf(RegraDeNegocioException.class)
            .hasMessageContaining("não está como pré-admissional");
    }

    @Test
    void efetivarAdmissional_atualizaStatusParaAtivo() {
        Funcionario func = new Funcionario();
        func.setId(30L);
        func.setNome("Carlos");
        func.setStatus(StatusFuncionario.PRE_ADMISSIONAL);
        func.setAtivo(true);

        when(funcionarioRepo.findById(30L)).thenReturn(Optional.of(func));
        when(funcionarioRepo.findByMatricula("MAT999")).thenReturn(Optional.empty());
        when(funcionarioRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Funcionario resultado = funcionarioService.efetivarAdmissional(30L, "MAT999");

        assertThat(resultado.getStatus()).isEqualTo(StatusFuncionario.ATIVO);
        assertThat(resultado.getMatricula()).isEqualTo("MAT999");
    }

    @Test
    void efetivarAdmissional_lancaExcecao_quandoMatriculaJaEmUso() {
        Funcionario preAdm = new Funcionario();
        preAdm.setId(30L);
        preAdm.setNome("Carlos");
        preAdm.setStatus(StatusFuncionario.PRE_ADMISSIONAL);

        Funcionario outroPorMatricula = funcionarioAtivo(999L, "Outro", "TI", "Dev");
        outroPorMatricula.setMatricula("MAT-DUPLICADA");

        when(funcionarioRepo.findById(30L)).thenReturn(Optional.of(preAdm));
        when(funcionarioRepo.findByMatricula("MAT-DUPLICADA")).thenReturn(Optional.of(outroPorMatricula));

        assertThatThrownBy(() -> funcionarioService.efetivarAdmissional(30L, "MAT-DUPLICADA"))
            .isInstanceOf(RegraDeNegocioException.class)
            .hasMessageContaining("já está em uso");
    }

    // ── helpers ───────────────────────────────────────────────────────

    private Funcionario funcionarioAtivo(Long id, String nome, String setor, String funcao) {
        Funcionario f = new Funcionario();
        f.setId(id);
        f.setNome(nome);
        f.setSetor(setor);
        f.setFuncao(funcao);
        f.setStatus(StatusFuncionario.ATIVO);
        f.setAtivo(true);
        return f;
    }
}
