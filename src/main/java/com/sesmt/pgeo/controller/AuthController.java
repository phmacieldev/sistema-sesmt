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
            Model model) {

        if (error != null)  model.addAttribute("erro", "Usuário ou senha inválidos.");
        if (logout != null) model.addAttribute("info", "Você saiu do sistema.");

        return "login";
    }
}
