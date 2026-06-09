package com.sesmt.pgeo.controller;

import com.sesmt.pgeo.audit.AuditLogRepository;
import com.sesmt.pgeo.config.SecurityConfig;
import com.sesmt.pgeo.model.enums.Role;
import com.sesmt.pgeo.repository.UsuarioRepository;
import com.sesmt.pgeo.service.LoginAttemptService;
import com.sesmt.pgeo.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UsuarioController.class)
@Import(SecurityConfig.class)
class UsuarioControllerTest {

    @Autowired MockMvc mvc;

    // Deps do UsuarioController
    @MockBean UsuarioService      usuarioService;
    @MockBean AuditLogRepository  auditLogRepo;

    // Deps do SecurityConfig
    @MockBean UsuarioRepository   usuarioRepo;
    @MockBean LoginAttemptService loginAttemptService;

    @BeforeEach
    void setup() {
        when(usuarioService.listarTodos()).thenReturn(List.of());
        when(auditLogRepo.buscarComFiltros(any(), any(), any(),
                any(LocalDateTime.class), any(LocalDateTime.class), any(Pageable.class)))
            .thenReturn(Page.empty());
        when(auditLogRepo.buscarComFiltros(any(), any(), any(),
                isNull(), isNull(), any(Pageable.class)))
            .thenReturn(Page.empty());
        when(auditLogRepo.findDistinctEntidades()).thenReturn(List.of());
        when(auditLogRepo.findDistinctAcoes()).thenReturn(List.of());
    }

    // ── Segurança: /admin/** requer ADMIN ─────────────────────────────

    @Test
    void listarUsuarios_semAutenticacao_redirecionaParaLogin() throws Exception {
        mvc.perform(get("/admin/usuarios"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void listarUsuarios_roleOperador_retorna403() throws Exception {
        mvc.perform(get("/admin/usuarios"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listarUsuarios_roleAdmin_retorna200() throws Exception {
        mvc.perform(get("/admin/usuarios"))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/usuarios"))
            .andExpect(model().attributeExists("usuarios"))
            .andExpect(model().attributeExists("roles"));
    }

    // ── Auditoria ─────────────────────────────────────────────────────

    @Test
    void auditoria_semAutenticacao_redirecionaParaLogin() throws Exception {
        mvc.perform(get("/admin/auditoria"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void auditoria_roleOperador_retorna403() throws Exception {
        mvc.perform(get("/admin/auditoria"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void auditoria_roleAdmin_retorna200() throws Exception {
        mvc.perform(get("/admin/auditoria"))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/auditoria"))
            .andExpect(model().attributeExists("logs"))
            .andExpect(model().attributeExists("totalPaginas"))
            .andExpect(model().attributeExists("entidades"))
            .andExpect(model().attributeExists("acoes"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void auditoria_comFiltros_retorna200() throws Exception {
        mvc.perform(get("/admin/auditoria")
                .param("usuario",     "admin")
                .param("entidade",    "Agendamento")
                .param("acao",        "CRIAR")
                .param("data_inicio", "2026-01-01")
                .param("data_fim",    "2026-06-30"))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/auditoria"));
    }

    // ── CRUD de usuários ──────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void criarUsuario_sucesso_redirecionaComMensagem() throws Exception {
        when(usuarioService.criar(any(), any(), any(), any(Role.class)))
            .thenReturn(new com.sesmt.pgeo.model.Usuario());

        mvc.perform(post("/admin/usuarios/criar").with(csrf())
                .param("username",     "novo_user")
                .param("nomeCompleto", "Novo Usuário")
                .param("senha",        "Senha@123")
                .param("role",         "OPERADOR"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/usuarios"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void criarUsuario_erroNegocio_redirecionaComErro() throws Exception {
        doThrow(new com.sesmt.pgeo.exception.RegraDeNegocioException("Usuário já existe"))
            .when(usuarioService).criar(any(), any(), any(), any(Role.class));

        mvc.perform(post("/admin/usuarios/criar").with(csrf())
                .param("username",     "admin")
                .param("nomeCompleto", "Admin")
                .param("senha",        "Admin@123")
                .param("role",         "ADMIN"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/usuarios"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void alterarRole_sucesso_redireciona() throws Exception {
        doNothing().when(usuarioService).alterarRole(anyLong(), any(Role.class));

        mvc.perform(post("/admin/usuarios/1/role").with(csrf())
                .param("role", "VISUALIZADOR"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/usuarios"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void alterarStatus_desativar_redireciona() throws Exception {
        doNothing().when(usuarioService).alterarStatus(anyLong(), anyBoolean());

        mvc.perform(post("/admin/usuarios/1/status").with(csrf())
                .param("ativo", "false"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/usuarios"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void resetarSenha_sucesso_redireciona() throws Exception {
        doNothing().when(usuarioService).resetarSenha(anyLong(), any());

        mvc.perform(post("/admin/usuarios/1/reset-senha").with(csrf())
                .param("novaSenha", "NovaSenha@123"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/usuarios"));
    }

    // ── Minha senha ───────────────────────────────────────────────────

    @Test
    void minhaSenha_semAutenticacao_redirecionaParaLogin() throws Exception {
        mvc.perform(get("/minha-senha"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser
    void minhaSenha_autenticado_retorna200() throws Exception {
        mvc.perform(get("/minha-senha"))
            .andExpect(status().isOk())
            .andExpect(view().name("minha_senha"));
    }

    @Test
    @WithMockUser
    void minhaSenhaPost_senhasDivergentes_redirecionaComErro() throws Exception {
        mvc.perform(post("/minha-senha").with(csrf())
                .param("senhaAtual",      "Atual@123")
                .param("novaSenha",       "Nova@123")
                .param("confirmarSenha",  "Diferente@123"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/minha-senha"));
    }

    @Test
    @WithMockUser
    void minhaSenhaPost_sucesso_redirecionaComMensagem() throws Exception {
        doNothing().when(usuarioService).trocarSenha(any(), any(), any());

        mvc.perform(post("/minha-senha").with(csrf())
                .param("senhaAtual",     "Atual@123")
                .param("novaSenha",      "Nova@456")
                .param("confirmarSenha", "Nova@456"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/minha-senha"));
    }
}
