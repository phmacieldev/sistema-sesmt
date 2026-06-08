package com.sesmt.pgeo.controller;

import com.sesmt.pgeo.exception.RegraDeNegocioException;
import com.sesmt.pgeo.model.Usuario;
import com.sesmt.pgeo.model.enums.Role;
import com.sesmt.pgeo.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    // ── Gestão de usuários (só ADMIN) ───────────────────────────────

    @GetMapping("/admin/usuarios")
    @PreAuthorize("hasRole('ADMIN')")
    public String listar(Model model) {
        model.addAttribute("usuarios", usuarioService.listarTodos());
        model.addAttribute("roles", Role.values());
        return "admin/usuarios";
    }

    @PostMapping("/admin/usuarios/criar")
    @PreAuthorize("hasRole('ADMIN')")
    public String criar(
            @RequestParam String username,
            @RequestParam String nomeCompleto,
            @RequestParam String senha,
            @RequestParam Role role,
            RedirectAttributes redirect) {
        try {
            usuarioService.criar(username, nomeCompleto, senha, role);
            redirect.addFlashAttribute("mensagem",
                "Usuário '" + username + "' criado com sucesso.");
        } catch (RegraDeNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/admin/usuarios";
    }

    @PostMapping("/admin/usuarios/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public String alterarRole(
            @PathVariable Long id,
            @RequestParam Role role,
            RedirectAttributes redirect) {
        try {
            usuarioService.alterarRole(id, role);
            redirect.addFlashAttribute("mensagem", "Perfil atualizado.");
        } catch (RegraDeNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/admin/usuarios";
    }

    @PostMapping("/admin/usuarios/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public String alterarStatus(
            @PathVariable Long id,
            @RequestParam boolean ativo,
            RedirectAttributes redirect) {
        try {
            usuarioService.alterarStatus(id, ativo);
            redirect.addFlashAttribute("mensagem",
                ativo ? "Usuário reativado." : "Usuário desativado.");
        } catch (RegraDeNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/admin/usuarios";
    }

    @PostMapping("/admin/usuarios/{id}/reset-senha")
    @PreAuthorize("hasRole('ADMIN')")
    public String resetarSenha(
            @PathVariable Long id,
            @RequestParam String novaSenha,
            RedirectAttributes redirect) {
        try {
            usuarioService.resetarSenha(id, novaSenha);
            redirect.addFlashAttribute("mensagem", "Senha redefinida com sucesso.");
        } catch (RegraDeNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/admin/usuarios";
    }

    // ── Troca de senha pelo próprio usuário ─────────────────────────

    @GetMapping("/minha-senha")
    public String minhaSenhaForm() {
        return "minha_senha";
    }

    @PostMapping("/minha-senha")
    public String minhaSenhaPost(
            Authentication auth,
            @RequestParam String senhaAtual,
            @RequestParam String novaSenha,
            @RequestParam String confirmarSenha,
            RedirectAttributes redirect) {

        if (!novaSenha.equals(confirmarSenha)) {
            redirect.addFlashAttribute("erro", "Nova senha e confirmação não coincidem.");
            return "redirect:/minha-senha";
        }
        try {
            usuarioService.trocarSenha(auth.getName(), senhaAtual, novaSenha);
            redirect.addFlashAttribute("mensagem", "Senha alterada com sucesso!");
        } catch (RegraDeNegocioException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/minha-senha";
    }
}
