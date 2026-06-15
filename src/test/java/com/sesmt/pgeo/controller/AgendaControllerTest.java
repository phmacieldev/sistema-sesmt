/*
 * Copyright (c) 2026 Pedro Henrique Maciel da Silva Faria. Todos os direitos reservados.
 * Desenvolvido de forma independente como projeto de portfólio.
 * Autorizado apenas para uso interno homologado.
 */
package com.sesmt.pgeo.controller;

import com.sesmt.pgeo.config.SecurityConfig;
import com.sesmt.pgeo.model.Agendamento;
import com.sesmt.pgeo.model.enums.TipoExame;
import com.sesmt.pgeo.repository.AgendamentoRepository;
import com.sesmt.pgeo.repository.FuncionarioRepository;
import com.sesmt.pgeo.repository.UsuarioRepository;
import com.sesmt.pgeo.service.AgendamentoService;
import com.sesmt.pgeo.service.LoginAttemptService;
import com.sesmt.pgeo.service.PdfService;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AgendaController.class)
@Import(SecurityConfig.class)
class AgendaControllerTest {

    @Autowired MockMvc mvc;

    // Deps do AgendaController
    @MockBean AgendamentoRepository agendamentoRepo;
    @MockBean FuncionarioRepository  funcionarioRepo;
    @MockBean AgendamentoService     agendamentoService;
    @MockBean PdfService             pdfService;

    // Deps do SecurityConfig
    @MockBean UsuarioRepository   usuarioRepo;
    @MockBean LoginAttemptService loginAttemptService;

    @BeforeEach
    void setup() {
        when(agendamentoRepo.findAllByOrderByDataClinicoAsc()).thenReturn(List.of());
        when(agendamentoService.getHorariosDisponiveis()).thenReturn(List.of("08:00", "09:00"));
        when(agendamentoService.getHorariosDisponiveisPorData(any())).thenReturn(List.of("08:00"));
    }

    // ── Segurança ─────────────────────────────────────────────────────

    @Test
    void agenda_semAutenticacao_redirecionaParaLogin() throws Exception {
        mvc.perform(get("/agenda"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void agendar_semAutenticacao_redirecionaParaLogin() throws Exception {
        mvc.perform(get("/agendar"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
    }

    // ── Endpoints autenticados ────────────────────────────────────────

    @Test
    @WithMockUser
    void agenda_autenticado_retorna200() throws Exception {
        mvc.perform(get("/agenda"))
            .andExpect(status().isOk())
            .andExpect(view().name("agenda"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void agendar_comAdmin_retorna200() throws Exception {
        mvc.perform(get("/agendar"))
            .andExpect(status().isOk())
            .andExpect(view().name("agendar"));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void adminEndpoint_semRoleAdmin_retorna403() throws Exception {
        // Endpoint /admin/** requer ROLE_ADMIN — URL-level security
        mvc.perform(get("/admin/usuarios"))
            .andExpect(status().isForbidden());
    }

    // ── API de calendário ─────────────────────────────────────────────

    @Test
    @WithMockUser
    void agendaEventsJson_retornaListaVazia() throws Exception {
        mvc.perform(get("/agenda_events_json"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("application/json"));
    }

    // ── Autocomplete ──────────────────────────────────────────────────

    @Test
    @WithMockUser
    void buscarFuncionario_naoEncontrado_retornaEncontradoFalse() throws Exception {
        when(funcionarioRepo.findByMatricula(anyString())).thenReturn(Optional.empty());

        mvc.perform(get("/buscar_funcionario/999"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.encontrado").value(false));
    }

    @Test
    @WithMockUser
    void buscarFuncionarioNome_menorQue2chars_retornaListaVazia() throws Exception {
        mvc.perform(get("/buscar_funcionarios_nome").param("q", "a"))
            .andExpect(status().isOk())
            .andExpect(content().json("[]"));
    }

    // ── POST /agendar ─────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "OPERADOR")
    void agendarPost_tipoExameInvalido_retornaErro() throws Exception {
        mvc.perform(post("/agendar").with(csrf())
                .param("nome",         "João Silva")
                .param("tipo_exame",   "INVALIDO")
                .param("data_clinico", "2026-07-01")
                .param("hora",         "08:00"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.erro").value(true));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void agendarPost_sucesso_retornaOkComId() throws Exception {
        Agendamento ag = new Agendamento();
        ag.setId(42L);
        when(agendamentoService.criar(any(), any(), any(), any(),
                any(TipoExame.class), any(LocalDate.class), any(), any(), any(), any()))
            .thenReturn(ag);

        mvc.perform(post("/agendar").with(csrf())
                .param("nome",         "João Silva")
                .param("tipo_exame",   "Periódico")
                .param("data_clinico", "2026-07-01")
                .param("hora",         "08:00"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ok").value(true))
            .andExpect(jsonPath("$.id").value(42));
    }
}
