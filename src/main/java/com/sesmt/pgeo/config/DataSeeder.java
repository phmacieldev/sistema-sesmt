/*
 * Copyright (c) 2026 Pedro Henrique Maciel da Silva Faria. Todos os direitos reservados.
 * Desenvolvido de forma independente como projeto de portfólio.
 * Autorizado apenas para uso interno homologado.
 */
package com.sesmt.pgeo.config;

import com.sesmt.pgeo.model.Usuario;
import com.sesmt.pgeo.model.enums.Role;
import com.sesmt.pgeo.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Cria usuários padrão na primeira execução.
 * Senhas vêm de ADMIN_PASSWORD/OPERADOR_PASSWORD/VISUALIZADOR_PASSWORD (com fallback
 * para uso em dev/local) — troque-as em produção antes de expor a aplicação.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UsuarioRepository usuarioRepo;
    private final PasswordEncoder passwordEncoder;

    @Value("${pgeo.seed.admin.senha}")
    private String senhaAdmin;

    @Value("${pgeo.seed.operador.senha}")
    private String senhaOperador;

    @Value("${pgeo.seed.visualizador.senha}")
    private String senhaVisualizador;

    @Value("${spring.profiles.active:}")
    private String perfilAtivo;

    /** Fallbacks de dev definidos em application.properties — proibidos em produção. */
    private static final java.util.Set<String> SENHAS_DEV =
        java.util.Set.of("admin@123", "oper@123", "view@123");

    @Override
    public void run(String... args) {
        if (usuarioRepo.count() == 0) {
            if (perfilAtivo.contains("prod")
                    && (SENHAS_DEV.contains(senhaAdmin)
                     || SENHAS_DEV.contains(senhaOperador)
                     || SENHAS_DEV.contains(senhaVisualizador))) {
                throw new IllegalStateException(
                    "Senhas padrão de desenvolvimento detectadas no perfil prod. " +
                    "Defina ADMIN_PASSWORD, OPERADOR_PASSWORD e VISUALIZADOR_PASSWORD " +
                    "no ambiente antes de iniciar a aplicação.");
            }
            criarUsuario("admin",       "Admin SESMT",    senhaAdmin,       Role.ADMIN);
            criarUsuario("operador",    "Operador SESMT", senhaOperador,    Role.OPERADOR);
            criarUsuario("visualizador","Visualizador",   senhaVisualizador,Role.VISUALIZADOR);

            log.warn("╔══════════════════════════════════════════════════════╗");
            log.warn("║  USUÁRIOS PADRÃO CRIADOS — TROQUE AS SENHAS!         ║");
            log.warn("║  admin / operador / visualizador                     ║");
            log.warn("║  Senhas definidas via ADMIN_PASSWORD / OPERADOR_PASSWORD ║");
            log.warn("║  / VISUALIZADOR_PASSWORD (ou padrão de dev, se ausentes) ║");
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
