/*
 * Copyright (c) 2026 Pedro Henrique Maciel da Silva Faria. Todos os direitos reservados.
 * Desenvolvido de forma independente como projeto de portfólio.
 * Autorizado apenas para uso interno homologado.
 */
package com.sesmt.pgeo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller de Autenticação.
 *
 * Equivalente ao Blueprint auth_bp (auth_routes.py).
 *
 * O Spring Security já cuida do POST /login automaticamente.
 * Só precisamos da página GET /login.
 */
@Controller
public class AuthController {

    @GetMapping("/login")
    public String login(
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String logout,
            @RequestParam(required = false) String bloqueado,
            Model model) {

        if (error != null)     model.addAttribute("erro", "Usuário ou senha inválidos.");
        if (logout != null)    model.addAttribute("info", "Você saiu do sistema.");
        if (bloqueado != null) model.addAttribute("erro",
            "Muitas tentativas de login. Aguarde 15 minutos e tente novamente.");

        return "login";
    }
}
