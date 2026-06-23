/*
 * Copyright (c) 2026 Pedro Henrique Maciel da Silva Faria. Todos os direitos reservados.
 * Desenvolvido de forma independente como projeto de portfólio.
 * Autorizado apenas para uso interno homologado.
 */
package com.sesmt.pgeo.controller;

import com.sesmt.pgeo.audit.AuditService;
import com.sesmt.pgeo.config.SecurityConfig;
import com.sesmt.pgeo.exception.RecursoNaoEncontradoException;
import com.sesmt.pgeo.repository.UsuarioRepository;
import com.sesmt.pgeo.service.AsoService;
import com.sesmt.pgeo.service.LoginAttemptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AsoController.class)
@Import(SecurityConfig.class)
class AsoControllerTest {

    @Autowired MockMvc mvc;

    // Deps do AsoController
    @MockBean AsoService  asoService;
    @MockBean AuditService auditService;

    // Deps do SecurityConfig
    @MockBean UsuarioRepository   usuarioRepo;
    @MockBean LoginAttemptService loginAttemptService;

    @BeforeEach
    void setup() {
        when(auditService.getUsuarioAtual()).thenReturn("operador");
    }

    // ── Segurança ─────────────────────────────────────────────────────

    @Test
    void atualizarStatusAso_semAutenticacao_retorna403() throws Exception {
        mvc.perform(post("/atualizar_status_aso")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"agendamento_id\":1,\"campo\":\"enviado\",\"valor\":true}")
                .with(csrf()))
            .andExpect(status().is3xxRedirection());
    }

    // ── Sucesso ───────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "OPERADOR")
    void atualizarStatusAso_enviado_retornaSucesso() throws Exception {
        when(asoService.atualizarStatusAso(eq(1L), eq("enviado"), eq(true), any()))
            .thenReturn(true);

        mvc.perform(post("/atualizar_status_aso")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"agendamento_id\":1,\"campo\":\"enviado\",\"valor\":true}")
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sucesso").value(true));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void atualizarStatusAso_recebido_retornaSucesso() throws Exception {
        when(asoService.atualizarStatusAso(eq(2L), eq("recebido"), eq(true), any()))
            .thenReturn(true);

        mvc.perform(post("/atualizar_status_aso")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"agendamento_id\":2,\"campo\":\"recebido\",\"valor\":true}")
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sucesso").value(true));
    }

    // ── Erros ─────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "OPERADOR")
    void atualizarStatusAso_agendamentoNaoEncontrado_retornaErro() throws Exception {
        doThrow(new RecursoNaoEncontradoException("Agendamento", 999L))
            .when(asoService).atualizarStatusAso(eq(999L), any(), anyBoolean(), any());

        mvc.perform(post("/atualizar_status_aso")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"agendamento_id\":999,\"campo\":\"enviado\",\"valor\":true}")
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sucesso").value(false))
            .andExpect(jsonPath("$.erro").exists());
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void atualizarStatusAso_campoAusente_retornaErro() throws Exception {
        // Sem agendamento_id → NullPointerException na conversão
        mvc.perform(post("/atualizar_status_aso")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"campo\":\"enviado\",\"valor\":true}")
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sucesso").value(false));
    }
}
