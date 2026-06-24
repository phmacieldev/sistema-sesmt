/*
 * Copyright (c) 2026 Pedro Henrique Maciel da Silva Faria. Todos os direitos reservados.
 * Desenvolvido de forma independente como projeto de portfólio.
 * Autorizado apenas para uso interno homologado.
 */
package com.sesmt.pgeo.controller;

import com.sesmt.pgeo.config.SecurityConfig;
import com.sesmt.pgeo.model.Funcionario;
import com.sesmt.pgeo.model.MedicalLeave;
import com.sesmt.pgeo.model.enums.TipoAtestado;
import com.sesmt.pgeo.repository.FuncionarioRepository;
import com.sesmt.pgeo.repository.MedicalLeaveRepository;
import com.sesmt.pgeo.repository.UsuarioRepository;
import com.sesmt.pgeo.service.AtestadoPdfService;
import com.sesmt.pgeo.service.AtestadoService;
import com.sesmt.pgeo.service.LoginAttemptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AtestadoController.class)
@Import(SecurityConfig.class)
class AtestadoControllerTest {

    @Autowired MockMvc mvc;

    @MockBean MedicalLeaveRepository repo;
    @MockBean FuncionarioRepository   funcRepo;
    @MockBean AtestadoService         service;
    @MockBean AtestadoPdfService      pdfService;

    @MockBean UsuarioRepository   usuarioRepo;
    @MockBean LoginAttemptService loginAttemptService;

    private Funcionario func;
    private MedicalLeave atestado;

    @BeforeEach
    void setup() {
        func = new Funcionario();
        func.setId(1L);
        func.setNome("Maria Souza");
        func.setSetor("Enfermagem");
        func.setFuncao("Enfermeira");

        atestado = new MedicalLeave();
        atestado.setId(1L);
        atestado.setFuncionario(func);
        atestado.setDataAfastamento(LocalDate.now());
        atestado.setDiasAfastamento(3);
        atestado.setTipo(TipoAtestado.DOENCA);

        LocalDate inicio = LocalDate.now().minusDays(3);
        LocalDate fim    = LocalDate.now().plusDays(3);

        when(service.semanaInicio(any())).thenReturn(inicio);
        when(service.semanaFim(any())).thenReturn(fim);
        when(service.buscarPorId(1L)).thenReturn(atestado);
        when(service.buscarPorId(999L)).thenThrow(
            new com.sesmt.pgeo.exception.RecursoNaoEncontradoException("Atestado", 999L));
        when(repo.findBySemanaOrdemLancamento(any(), any())).thenReturn(List.of(atestado));
        when(repo.findBySemana(any(), any())).thenReturn(List.of(atestado));
        when(service.totalPorSetor(any())).thenReturn(Map.of("Enfermagem", 3));
        when(service.totalPorTipo(any())).thenReturn(Map.of("DOENCA", 1L));
    }

    // ── Segurança ─────────────────────────────────────────────────────

    @Test
    void lista_semAutenticacao_redirecionaParaLogin() throws Exception {
        mvc.perform(get("/atestados"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void novoForm_semAutenticacao_redirecionaParaLogin() throws Exception {
        mvc.perform(get("/atestados/novo"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
    }

    // ── Lista semanal ─────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "OPERADOR")
    void lista_semParametros_retorna200ComViewCorreta() throws Exception {
        mvc.perform(get("/atestados"))
            .andExpect(status().isOk())
            .andExpect(view().name("atestados/lista"))
            .andExpect(model().attributeExists("atestados"))
            .andExpect(model().attributeExists("semanaInicio"))
            .andExpect(model().attributeExists("semanaFim"))
            .andExpect(model().attributeExists("semanaAtualInicio"));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void lista_comParametroSemana_usaSemanaFornecida() throws Exception {
        mvc.perform(get("/atestados").param("semana", "2026-06-10"))
            .andExpect(status().isOk())
            .andExpect(view().name("atestados/lista"));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void lista_comFiltroNome_usaBuscaPorNome() throws Exception {
        when(repo.findBySemanaENome(any(), any(), eq("maria"))).thenReturn(List.of(atestado));

        mvc.perform(get("/atestados").param("nome", "maria"))
            .andExpect(status().isOk())
            .andExpect(view().name("atestados/lista"));
    }

    // ── Busca global ──────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "OPERADOR")
    void busca_comNome_retornaFragmento() throws Exception {
        when(repo.findByNomeTodos("maria")).thenReturn(List.of(atestado));

        mvc.perform(get("/atestados/busca").param("nome", "maria"))
            .andExpect(status().isOk())
            .andExpect(view().name("atestados/lista :: tabelaAtestados"));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void busca_semNome_retornaListaVazia() throws Exception {
        mvc.perform(get("/atestados/busca"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("atestados", List.of()));
    }

    // ── Formulário de lançamento ──────────────────────────────────────

    @Test
    @WithMockUser(roles = "OPERADOR")
    void novoForm_autenticado_retorna200() throws Exception {
        mvc.perform(get("/atestados/novo"))
            .andExpect(status().isOk())
            .andExpect(view().name("atestados/form"))
            .andExpect(model().attributeExists("tipos"))
            .andExpect(model().attribute("edicao", false));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void novoPost_sucesso_redirecionaParaSemana() throws Exception {
        when(funcRepo.findById(1L)).thenReturn(Optional.of(func));
        when(repo.save(any())).thenReturn(atestado);

        mvc.perform(post("/atestados/novo").with(csrf())
                .param("funcionarioId",   "1")
                .param("dataAfastamento", "2026-06-10")
                .param("diasAfastamento", "3")
                .param("tipo",            "DOENCA"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("/atestados*"));
    }

    // ── Edição ────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "OPERADOR")
    void editarForm_encontrado_retorna200() throws Exception {
        when(repo.findById(1L)).thenReturn(Optional.of(atestado));

        mvc.perform(get("/atestados/1/editar"))
            .andExpect(status().isOk())
            .andExpect(view().name("atestados/form"))
            .andExpect(model().attribute("edicao", true));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void editarForm_naoEncontrado_retorna404() throws Exception {
        when(repo.findById(999L)).thenReturn(Optional.empty());

        mvc.perform(get("/atestados/999/editar"))
            .andExpect(status().isOk())
            .andExpect(view().name("error/erro"))
            .andExpect(model().attribute("status", 404));
    }

    // ── Exclusão ──────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "OPERADOR")
    void excluir_sucesso_redirecionaParaSemana() throws Exception {
        when(repo.findById(1L)).thenReturn(Optional.of(atestado));

        mvc.perform(post("/atestados/1/excluir").with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("/atestados*"));
    }

    // ── Indicadores ───────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "OPERADOR")
    void indicadores_retorna200ComModel() throws Exception {
        when(repo.findUltimos60Dias(any())).thenReturn(List.of());
        when(service.resumoPor60Dias(any())).thenReturn(List.of());

        mvc.perform(get("/atestados/indicadores"))
            .andExpect(status().isOk())
            .andExpect(view().name("atestados/indicadores"))
            .andExpect(model().attributeExists("totalDias"))
            .andExpect(model().attributeExists("totalAtestados"));
    }

    // ── PDF ───────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "OPERADOR")
    void pdf_retornaBytesComContentTypePdf() throws Exception {
        when(pdfService.gerarRelatorio(any(), any(), any(), any(), any()))
            .thenReturn(new byte[]{1, 2, 3});

        mvc.perform(get("/atestados/pdf"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "application/pdf"));
    }
}
