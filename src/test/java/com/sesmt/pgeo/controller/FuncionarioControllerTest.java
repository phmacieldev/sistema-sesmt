package com.sesmt.pgeo.controller;

import com.sesmt.pgeo.config.SecurityConfig;
import com.sesmt.pgeo.model.Funcionario;
import com.sesmt.pgeo.model.enums.StatusFuncionario;
import com.sesmt.pgeo.repository.AgendamentoRepository;
import com.sesmt.pgeo.repository.FuncionarioRepository;
import com.sesmt.pgeo.repository.HistoricoCargoRepository;
import com.sesmt.pgeo.repository.UsuarioRepository;
import com.sesmt.pgeo.service.FuncionarioService;
import com.sesmt.pgeo.service.LoginAttemptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FuncionarioController.class)
@Import(SecurityConfig.class)
class FuncionarioControllerTest {

    @Autowired MockMvc mvc;

    // Deps do FuncionarioController
    @MockBean FuncionarioRepository    funcionarioRepo;
    @MockBean HistoricoCargoRepository historicoRepo;
    @MockBean AgendamentoRepository    agendamentoRepo;
    @MockBean FuncionarioService       funcionarioService;

    // Deps do SecurityConfig
    @MockBean UsuarioRepository   usuarioRepo;
    @MockBean LoginAttemptService loginAttemptService;

    private Funcionario funcionarioMock;

    @BeforeEach
    void setup() {
        funcionarioMock = new Funcionario();
        funcionarioMock.setId(1L);
        funcionarioMock.setNome("João Silva");
        funcionarioMock.setStatus(StatusFuncionario.ATIVO);
        funcionarioMock.setSetor("TI");
        funcionarioMock.setFuncao("Analista");
        funcionarioMock.setExigeSangue(true);

        when(funcionarioRepo.findById(1L)).thenReturn(Optional.of(funcionarioMock));
        when(historicoRepo.findByFuncionarioIdOrderByAlteradoEmDesc(1L)).thenReturn(List.of());
        when(agendamentoRepo.findByFuncionarioIdOrderByDataClinicoDesc(1L)).thenReturn(List.of());
    }

    // ── Segurança ─────────────────────────────────────────────────────

    @Test
    void perfil_semAutenticacao_redirecionaParaLogin() throws Exception {
        mvc.perform(get("/funcionario/1"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void apiFuncionario_semAutenticacao_redirecionaParaLogin() throws Exception {
        mvc.perform(get("/api/funcionario/12345"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
    }

    // ── Perfil ────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "OPERADOR")
    void perfil_autenticado_retorna200ComViewCorreta() throws Exception {
        mvc.perform(get("/funcionario/1"))
            .andExpect(status().isOk())
            .andExpect(view().name("funcionario/perfil"))
            .andExpect(model().attributeExists("funcionario"))
            .andExpect(model().attributeExists("historicoCargo"))
            .andExpect(model().attributeExists("agendamentos"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void perfil_funcionarioNaoEncontrado_retornaErro404() throws Exception {
        when(funcionarioRepo.findById(999L)).thenReturn(Optional.empty());

        mvc.perform(get("/funcionario/999"))
            .andExpect(status().isOk())
            .andExpect(view().name("error/erro"))
            .andExpect(model().attribute("status", 404));
    }

    // ── Alterar cargo ─────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "OPERADOR")
    void alterarCargo_sucesso_redirecionaParaPerfil() throws Exception {
        when(funcionarioService.alterarCargo(eq(1L), any(), any(), anyBoolean(), any()))
            .thenReturn(funcionarioMock);

        mvc.perform(post("/funcionario/1/alterar-cargo").with(csrf())
                .param("novoSetor",  "RH")
                .param("novaFuncao", "Assistente")
                .param("exigeSangue","true")
                .param("motivo",     "TRANSFERENCIA"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/funcionario/1"));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void alterarCargo_mudancaDeRisco_adicionaAviso() throws Exception {
        when(funcionarioService.alterarCargo(eq(1L), any(), any(), anyBoolean(), any()))
            .thenReturn(funcionarioMock);

        mvc.perform(post("/funcionario/1/alterar-cargo").with(csrf())
                .param("novoSetor",  "Almoxarifado")
                .param("novaFuncao", "Estoquista")
                .param("exigeSangue","true")
                .param("motivo",     "MUDANCA_DE_RISCO"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/funcionario/1"));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void alterarCargo_erroNegocio_redirecionaComErro() throws Exception {
        doThrow(new com.sesmt.pgeo.exception.RegraDeNegocioException("Setor inválido"))
            .when(funcionarioService).alterarCargo(any(), any(), any(), anyBoolean(), any());

        mvc.perform(post("/funcionario/1/alterar-cargo").with(csrf())
                .param("novoSetor",  "")
                .param("novaFuncao", "Analista")
                .param("exigeSangue","true")
                .param("motivo",     "CORRECAO"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/funcionario/1"));
    }

    // ── Efetivar admissional ──────────────────────────────────────────

    @Test
    @WithMockUser(roles = "OPERADOR")
    void efetivar_sucesso_redirecionaParaPerfil() throws Exception {
        when(funcionarioService.efetivarAdmissional(eq(1L), any())).thenReturn(funcionarioMock);

        mvc.perform(post("/funcionario/1/efetivar").with(csrf())
                .param("matriculaReal", "00123"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/funcionario/1"));
    }

    // ── API JSON ──────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "OPERADOR")
    void apiFuncionario_encontrado_retornaDadosJson() throws Exception {
        when(funcionarioRepo.findByMatricula("12345")).thenReturn(Optional.of(funcionarioMock));

        mvc.perform(get("/api/funcionario/12345"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.encontrado").value(true))
            .andExpect(jsonPath("$.nome").value("João Silva"))
            .andExpect(jsonPath("$.status").value("ATIVO"));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void apiFuncionario_naoEncontrado_retornaEncontradoFalse() throws Exception {
        when(funcionarioRepo.findByMatricula("99999")).thenReturn(Optional.empty());

        mvc.perform(get("/api/funcionario/99999"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.encontrado").value(false));
    }
}
