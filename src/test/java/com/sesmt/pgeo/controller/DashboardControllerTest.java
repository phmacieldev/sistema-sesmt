/*
 * Copyright (c) 2026 Pedro Henrique Maciel da Silva Faria. Todos os direitos reservados.
 * Desenvolvido de forma independente como projeto de portfólio.
 * Autorizado apenas para uso interno homologado.
 */
package com.sesmt.pgeo.controller;

import com.sesmt.pgeo.config.SecurityConfig;
import com.sesmt.pgeo.repository.AgendamentoRepository;
import com.sesmt.pgeo.repository.FuncionarioRepository;
import com.sesmt.pgeo.repository.MedicalLeaveRepository;
import com.sesmt.pgeo.repository.UsuarioRepository;
import com.sesmt.pgeo.service.ExcelService;
import com.sesmt.pgeo.service.LoginAttemptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardController.class)
@Import(SecurityConfig.class)
class DashboardControllerTest {

    @Autowired MockMvc mvc;

    // Deps do DashboardController
    @MockBean AgendamentoRepository agendamentoRepo;
    @MockBean FuncionarioRepository  funcionarioRepo;
    @MockBean MedicalLeaveRepository medicalLeaveRepo;
    @MockBean ExcelService           excelService;

    // Deps do SecurityConfig
    @MockBean UsuarioRepository    usuarioRepo;
    @MockBean LoginAttemptService  loginAttemptService;

    @BeforeEach
    void setup() {
        when(agendamentoRepo.findAnosDisponiveis()).thenReturn(List.of(LocalDate.now().getYear()));
        when(agendamentoRepo.buscarComFiltros(any(), any(), any())).thenReturn(List.of());
        when(agendamentoRepo.buscarPaginado(any(), any(), any(), any(), any(Pageable.class)))
            .thenReturn(Page.empty());
        when(agendamentoRepo.findByMesEAnoPaginado(anyInt(), anyInt(), any(), any(), any(Pageable.class)))
            .thenReturn(Page.empty());
        when(excelService.gerarPlanilha(any())).thenReturn(new byte[]{});
    }

    // ── Segurança ─────────────────────────────────────────────────────

    @Test
    void dashboard_semAutenticacao_redirecionaParaLogin() throws Exception {
        mvc.perform(get("/dashboard"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void exportar_semAutenticacao_redirecionaParaLogin() throws Exception {
        mvc.perform(get("/exportar"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
    }

    // ── Dashboard principal ───────────────────────────────────────────

    @Test
    @WithMockUser
    void dashboard_autenticado_retorna200() throws Exception {
        mvc.perform(get("/dashboard"))
            .andExpect(status().isOk())
            .andExpect(view().name("dashboard"));
    }

    @Test
    @WithMockUser
    void dashboardDados_autenticado_retorna200() throws Exception {
        mvc.perform(get("/dashboard_dados")
                .param("mes", "6")
                .param("ano", "2026")
                .param("aplicar_filtro", "true"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void dashboardDados_semFiltro_usaBuscarPaginado() throws Exception {
        mvc.perform(get("/dashboard_dados"))
            .andExpect(status().isOk());
    }

    // ── Exportar ──────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void exportar_autenticado_retornaExcel() throws Exception {
        when(agendamentoRepo.findByMesEAnoExport(anyInt(), anyInt(), any(), any()))
            .thenReturn(List.of());
        when(excelService.gerarPlanilha(anyList())).thenReturn(new byte[100]);

        mvc.perform(get("/exportar")
                .param("mes", "6")
                .param("ano", "2026")
                .param("aplicar_filtro", "true"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(
                MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")));
    }

    @Test
    @WithMockUser
    void exportar_comIntervaloData_usaBuscarTodosExport() throws Exception {
        when(agendamentoRepo.buscarTodosExport(any(), any(LocalDate.class), any(LocalDate.class), any()))
            .thenReturn(List.of());
        when(excelService.gerarPlanilha(anyList())).thenReturn(new byte[50]);

        mvc.perform(get("/exportar")
                .param("data_inicio", "2026-01-01")
                .param("data_fim", "2026-06-30"))
            .andExpect(status().isOk());
    }

    // ── Outros endpoints ──────────────────────────────────────────────

    @Test
    @WithMockUser
    void raiz_redirecionaParaDashboard() throws Exception {
        mvc.perform(get("/"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/home"));
    }

    // ── Indicadores SESMT ─────────────────────────────────────────────

    @Test
    void indicadores_semAutenticacao_redirecionaParaLogin() throws Exception {
        mvc.perform(get("/indicadores"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser
    void indicadores_autenticado_retorna200ComModelCompleto() throws Exception {
        when(funcionarioRepo.findByAtivoTrue()).thenReturn(List.of());
        when(agendamentoRepo.findAll()).thenReturn(List.of());
        when(medicalLeaveRepo.findRecentes(any())).thenReturn(List.of());

        mvc.perform(get("/indicadores"))
            .andExpect(status().isOk())
            .andExpect(view().name("indicadores"))
            .andExpect(model().attributeExists("qtdVencidos"))
            .andExpect(model().attributeExists("qtdAVencer"))
            .andExpect(model().attributeExists("qtdEmDia"))
            .andExpect(model().attributeExists("qtdSemAso"))
            .andExpect(model().attributeExists("labelesMeses"))
            .andExpect(model().attributeExists("examesPorMes"))
            .andExpect(model().attributeExists("resultados"));
    }

    @Test
    @WithMockUser
    void indicadores_comParametroDias_filtraCorretamente() throws Exception {
        when(funcionarioRepo.findByAtivoTrue()).thenReturn(List.of());
        when(agendamentoRepo.findAll()).thenReturn(List.of());
        when(medicalLeaveRepo.findRecentes(any())).thenReturn(List.of());

        mvc.perform(get("/indicadores").param("dias", "30"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("dias", 30));
    }

    @Test
    @WithMockUser
    void indicadores_paginacaoValida_respeitaParametro() throws Exception {
        when(funcionarioRepo.findByAtivoTrue()).thenReturn(List.of());
        when(agendamentoRepo.findAll()).thenReturn(List.of());
        when(medicalLeaveRepo.findRecentes(any())).thenReturn(List.of());

        mvc.perform(get("/indicadores").param("pagina", "0"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("paginaAtual", 0))
            .andExpect(model().attribute("totalPaginas", 1));
    }

    // ── Vencimentos ASO ───────────────────────────────────────────────

    @Test
    void vencimentos_semAutenticacao_redirecionaParaLogin() throws Exception {
        mvc.perform(get("/vencimentos"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser
    void vencimentos_autenticado_retorna200ComModelCompleto() throws Exception {
        when(funcionarioRepo.findByAtivoTrue()).thenReturn(List.of());

        mvc.perform(get("/vencimentos"))
            .andExpect(status().isOk())
            .andExpect(view().name("vencimentos"))
            .andExpect(model().attributeExists("qtdVencidos"))
            .andExpect(model().attributeExists("qtdAVencer"))
            .andExpect(model().attributeExists("qtdEmDia"))
            .andExpect(model().attributeExists("qtdSemAso"))
            .andExpect(model().attributeExists("funcionarios"))
            .andExpect(model().attributeExists("statusFiltro"));
    }

    @Test
    @WithMockUser
    void vencimentos_comFiltroStatus_filtraVencidos() throws Exception {
        when(funcionarioRepo.findByAtivoTrue()).thenReturn(List.of());

        mvc.perform(get("/vencimentos").param("status", "vencidos"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("statusFiltro", "vencidos"));
    }
}
