package com.sesmt.pgeo.config;

import com.sesmt.pgeo.model.Usuario;
import com.sesmt.pgeo.model.enums.Role;
import com.sesmt.pgeo.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Cria usuários padrão na primeira execução.
 * Troque as senhas imediatamente após o primeiro acesso!
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UsuarioRepository usuarioRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (usuarioRepo.count() == 0) {
            criarUsuario("admin",       "Admin SESMT",    "admin@123",  Role.ADMIN);
            criarUsuario("operador",    "Operador SESMT", "oper@123",   Role.OPERADOR);
            criarUsuario("visualizador","Visualizador",   "view@123",   Role.VISUALIZADOR);

            log.warn("╔══════════════════════════════════════════════════════╗");
            log.warn("║  USUÁRIOS PADRÃO CRIADOS — TROQUE AS SENHAS!         ║");
            log.warn("║  admin / admin@123       → ADMIN (acesso total)      ║");
            log.warn("║  operador / oper@123     → OPERADOR                  ║");
            log.warn("║  visualizador / view@123 → VISUALIZADOR (read-only)  ║");
            log.warn("╚══════════════════════════════════════════════════════╝");
        }
    }

    private void criarUsuario(String username, String nomeCompleto, String senha, Role role) {
        Usuario u = new Usuario();
        u.setUsername(username);
        u.setNomeCompleto(nomeCompleto);
        u.setPassword(passwordEncoder.encode(senha));
        u.setRole(role);
        u.setAtivo(true);
        usuarioRepo.save(u);
    }
}
