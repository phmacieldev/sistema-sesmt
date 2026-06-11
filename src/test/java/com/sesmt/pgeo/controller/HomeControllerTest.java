/*
 * Copyright (c) 2026 Pedro Henrique Maciel da Silva Faria. Todos os direitos reservados.
 * Desenvolvido de forma independente como projeto de portfólio.
 * Autorizado apenas para uso interno homologado.
 */
package com.sesmt.pgeo.controller;

import com.sesmt.pgeo.config.SecurityConfig;
import com.sesmt.pgeo.model.Agendamento;
import com.sesmt.pgeo.model.Funcionario;
import com.sesmt.pgeo.model.MedicalLeave;
import com.sesmt.pgeo.repository.AgendamentoRepository;
import com.sesmt.pgeo.repository.FuncionarioRepository;
import com.sesmt.pgeo.repository.MedicalLeaveRepository;
import com.sesmt.pgeo.repository.UsuarioRepository;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HomeController.class)
@Import(SecurityConfig.class)
class HomeControllerTest {

    @Autowired MockMvc mvc;

    @MockBean AgendamentoRepository  agendamentoRepo;
    @MockBean FuncionarioRepository  funcionarioRepo;
    @MockBean MedicalLeaveRepository medicalLeaveRepo;

    @MockBean UsuarioRepository   usuarioRepo;
    @MockBean LoginAttemptService loginAttemptService;

    @BeforeEach
    void setup() {
        when(agendamentoRepo.findByMesEAno(anyInt(), anyInt())).thenReturn(List.of());
        when(agendamentoRepo.findAllByOrderByDataClinicoAsc()).thenReturn(List.of());
        when(funcionarioRepo.findByAsoVencendoAte(any())).thenReturn(List.of());
        when(medicalLeaveRepo.findRecentes(any())).thenReturn(List.of());
        when(medicalLeaveRepo.findDataMaisRecente()).thenReturn(Optional.empty());
    }

    // ── Segurança ─────────────────────────────────────────────────────

    @Test
    void home_semAutenticacao_redirecionaParaLogin() throws Exception {
        mvc.perform(get("/home"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
    }

    // ── Tela de início ────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "OPERADOR")
    void home_autenticado_retorna200ComModelCompleto() throws Exception {
        mvc.perform(get("/home"))
            .andExpect(status().isOk())
            .andExpect(view().name("home"))
            .andExpect(model().attributeExists("totalMes"))
            .andExpect(model().attributeExists("proximos7dias"))
            .andExpect(model().attributeExists("totalAsoVencidos"))
            .andExpect(model().attributeExists("totalAsoAVencer30"))
            .andExpect(model().attributeExists("totalAtestados60dias"))
            .andExpect(model().attributeExists("diasAfastamento"))
            .andExpect(model().attributeExists("proximosAgendamentos"))
            .andExpect(model().attributeExists("ultimosAtestados"))
            .andExpect(model().attributeExists("asoVencidos"))
            .andExpect(model().attributeExists("hoje"));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void home_semDados_retornaZerosNosKpis() throws Exception {
        mvc.perform(get("/home"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("totalMes", 0))
            .andExpect(model().attribute("totalAsoVencidos", 0))
            .andExpect(model().attribute("totalAtestados60dias", 0));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void home_comAsoVencidos_exibeNaLista() throws Exception {
        Funcionario func = new Funcionario();
        func.setId(1L);
        func.setNome("Carlos Oliveira");
        func.setSetor("UTI");
        func.setFuncao("Técnico");
        func.setAso(LocalDate.now().minusDays(10));

        when(funcionarioRepo.findByAsoVencendoAte(any())).thenReturn(List.of(func));

        mvc.perform(get("/home"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("totalAsoVencidos", 1));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void home_comAgendamentosNoMes_exibeTotalCorreto() throws Exception {
        Agendamento ag = new Agendamento();
        ag.setId(1L);
        ag.setFuncionarioNome("Teste");
        ag.setDataClinico(LocalDate.now());
        ag.setHoraClinico("09:00");

        when(agendamentoRepo.findByMesEAno(anyInt(), anyInt())).thenReturn(List.of(ag));

        mvc.perform(get("/home"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("totalMes", 1));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void home_paginacaoAso_respeitaParametro() throws Exception {
        mvc.perform(get("/home").param("paginaAso", "0"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("paginaAsoAtual", 0))
            .andExpect(model().attribute("totalPaginasAso", 1));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    void home_comAtestadosRecentes_exibeNaLista() throws Exception {
        Funcionario func = new Funcionario();
        func.setId(2L);
        func.setNome("Ana Lima");

        MedicalLeave ml = new MedicalLeave();
        ml.setId(1L);
        ml.setFuncionario(func);
        ml.setDataAfastamento(LocalDate.now().minusDays(5));
        ml.setDiasAfastamento(2);

        when(medicalLeaveRepo.findRecentes(any())).thenReturn(List.of(ml));

        mvc.perform(get("/home"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("totalAtestados60dias", 1));
    }
}
