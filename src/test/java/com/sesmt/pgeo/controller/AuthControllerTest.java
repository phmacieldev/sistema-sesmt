package com.sesmt.pgeo.controller;

import com.sesmt.pgeo.config.SecurityConfig;
import com.sesmt.pgeo.repository.UsuarioRepository;
import com.sesmt.pgeo.service.LoginAttemptService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired MockMvc mvc;

    // Deps do SecurityConfig
    @MockBean UsuarioRepository   usuarioRepo;
    @MockBean LoginAttemptService loginAttemptService;

    // ── GET /login ────────────────────────────────────────────────────

    @Test
    void login_semAutenticacao_retorna200() throws Exception {
        mvc.perform(get("/login"))
            .andExpect(status().isOk())
            .andExpect(view().name("login"));
    }

    @Test
    void login_comParametroError_expoeAtributoErro() throws Exception {
        mvc.perform(get("/login").param("error", ""))
            .andExpect(status().isOk())
            .andExpect(view().name("login"))
            .andExpect(model().attribute("erro", "Usuário ou senha inválidos."));
    }

    @Test
    void login_comParametroLogout_expoeAtributoInfo() throws Exception {
        mvc.perform(get("/login").param("logout", ""))
            .andExpect(status().isOk())
            .andExpect(view().name("login"))
            .andExpect(model().attribute("info", "Você saiu do sistema."));
    }

    @Test
    void login_comParametroBloqueado_expoeAtributoErroBloqueio() throws Exception {
        mvc.perform(get("/login").param("bloqueado", ""))
            .andExpect(status().isOk())
            .andExpect(view().name("login"))
            .andExpect(model().attribute("erro",
                containsString("Muitas tentativas de login")));
    }

    @Test
    void login_semParametros_semAtributosNoModel() throws Exception {
        mvc.perform(get("/login"))
            .andExpect(status().isOk())
            .andExpect(model().attributeDoesNotExist("erro"))
            .andExpect(model().attributeDoesNotExist("info"));
    }
}
