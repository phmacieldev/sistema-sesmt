/*
 * Copyright (c) 2026 Pedro Henrique Maciel da Silva Faria. Todos os direitos reservados.
 * Desenvolvido de forma independente como projeto de portfólio.
 * Autorizado apenas para uso interno homologado.
 */
package com.sesmt.pgeo.service;

import com.sesmt.pgeo.audit.AuditService;
import com.sesmt.pgeo.exception.RegraDeNegocioException;
import com.sesmt.pgeo.exception.RecursoNaoEncontradoException;
import com.sesmt.pgeo.model.Usuario;
import com.sesmt.pgeo.model.enums.Role;
import com.sesmt.pgeo.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepo;
    private final PasswordEncoder   passwordEncoder;
    private final AuditService      auditService;

    public List<Usuario> listarTodos() {
        return usuarioRepo.findAll();
    }

    public Usuario buscarPorId(Long id) {
        return usuarioRepo.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário", id));
    }

    @Transactional
    public Usuario criar(String username, String nomeCompleto, String senha, Role role) {
        if (usuarioRepo.findByUsername(username).isPresent()) {
            throw new RegraDeNegocioException("Já existe um usuário com o login '" + username + "'.");
        }
        if (senha == null || senha.length() < 8) {
            throw new RegraDeNegocioException("A senha deve ter no mínimo 8 caracteres.");
        }
        Usuario u = new Usuario();
        u.setUsername(username.strip().toLowerCase());
        u.setNomeCompleto(nomeCompleto);
        u.setPassword(passwordEncoder.encode(senha));
        u.setRole(role);
        u.setAtivo(true);
        Usuario salvo = usuarioRepo.save(u);
        auditService.registrarCriacao("Usuario", salvo.getId(),
            "Novo usuário criado: " + username + " [" + role + "]");
        return salvo;
    }

    @Transactional
    public void alterarRole(Long id, Role novaRole) {
        Usuario u = buscarPorId(id);
        Role anterior = u.getRole();
        u.setRole(novaRole);
        usuarioRepo.save(u);
        auditService.registrar("ALTERAR_ROLE", "Usuario", id,
            u.getUsername() + ": " + anterior + " → " + novaRole);
    }

    @Transactional
    public void alterarStatus(Long id, boolean ativo) {
        Usuario u = buscarPorId(id);
        // Impede desativar o único admin
        if (!ativo && u.getRole() == Role.ADMIN) {
            long adminsAtivos = usuarioRepo.findAll().stream()
                .filter(x -> x.getRole() == Role.ADMIN && x.isAtivo() && !x.getId().equals(id))
                .count();
            if (adminsAtivos == 0) {
                throw new RegraDeNegocioException(
                    "Não é possível desativar o único administrador do sistema.");
            }
        }
        u.setAtivo(ativo);
        usuarioRepo.save(u);
        auditService.registrar(ativo ? "ATIVAR_USUARIO" : "DESATIVAR_USUARIO",
            "Usuario", id, u.getUsername());
    }

    @Transactional
    public void resetarSenha(Long id, String novaSenha) {
        if (novaSenha == null || novaSenha.length() < 8) {
            throw new RegraDeNegocioException("A senha deve ter no mínimo 8 caracteres.");
        }
        Usuario u = buscarPorId(id);
        u.setPassword(passwordEncoder.encode(novaSenha));
        usuarioRepo.save(u);
        auditService.registrar("RESET_SENHA", "Usuario", id,
            "Senha resetada para: " + u.getUsername());
    }

    /** Troca de senha pelo próprio usuário — exige a senha atual */
    @Transactional
    public void trocarSenha(String username, String senhaAtual, String novaSenha) {
        Usuario u = usuarioRepo.findByUsername(username)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
        if (!passwordEncoder.matches(senhaAtual, u.getPassword())) {
            throw new RegraDeNegocioException("Senha atual incorreta.");
        }
        if (novaSenha == null || novaSenha.length() < 8) {
            throw new RegraDeNegocioException("A nova senha deve ter no mínimo 8 caracteres.");
        }
        u.setPassword(passwordEncoder.encode(novaSenha));
        usuarioRepo.save(u);
        auditService.registrar("TROCA_SENHA", "Usuario", u.getId(),
            u.getUsername() + " alterou sua própria senha");
    }
}
