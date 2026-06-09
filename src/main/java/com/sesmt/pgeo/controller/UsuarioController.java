package com.sesmt.pgeo.controller;

import com.sesmt.pgeo.audit.AuditLog;
import com.sesmt.pgeo.audit.AuditLogRepository;
import com.sesmt.pgeo.exception.RegraDeNegocioException;
import com.sesmt.pgeo.model.Usuario;
import com.sesmt.pgeo.model.enums.Role;
import com.sesmt.pgeo.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService     usuarioService;
    private final AuditLogRepository auditLogRepo;

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

    // ── Auditoria ───────────────────────────────────────────────────

    private static final int LOGS_POR_PAGINA = 50;

    @GetMapping("/admin/auditoria")
    @PreAuthorize("hasRole('ADMIN')")
    public String auditoria(
            @RequestParam(required = false) String usuario,
            @RequestParam(required = false) String entidade,
            @RequestParam(required = false) String acao,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data_inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data_fim,
            @RequestParam(defaultValue = "0") int pagina,
            Model model) {

        String u  = (usuario  != null && !usuario.isBlank())  ? usuario.strip()  : null;
        String e  = (entidade != null && !entidade.isBlank()) ? entidade.strip() : null;
        String ac = (acao     != null && !acao.isBlank())     ? acao.strip()     : null;

        LocalDateTime inicio = data_inicio != null ? data_inicio.atStartOfDay()           : null;
        LocalDateTime fim    = data_fim    != null ? data_fim.atTime(23, 59, 59)           : null;

        Page<AuditLog> page = auditLogRepo.buscarComFiltros(
            u, e, ac, inicio, fim,
            PageRequest.of(Math.max(0, pagina), LOGS_POR_PAGINA));

        model.addAttribute("logs",          page.getContent());
        model.addAttribute("totalPaginas",  page.getTotalPages());
        model.addAttribute("paginaAtual",   page.getNumber());
        model.addAttribute("totalItens",    page.getTotalElements());
        model.addAttribute("usuario",       usuario);
        model.addAttribute("entidade",      entidade);
        model.addAttribute("acao",          acao);
        model.addAttribute("dataInicio",    data_inicio);
        model.addAttribute("dataFim",       data_fim);
        model.addAttribute("entidades",     auditLogRepo.findDistinctEntidades());
        model.addAttribute("acoes",         auditLogRepo.findDistinctAcoes());
        return "admin/auditoria";
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
