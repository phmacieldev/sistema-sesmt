package com.sesmt.pgeo.model;

import com.sesmt.pgeo.model.enums.Role;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entidade Usuario — autenticação e autorização.
 *
 * Fase 1: adicionado campo role (perfil de acesso),
 * ativo (para desativar sem excluir) e ultimoLogin.
 */
@Entity
@Table(name = "usuario")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(nullable = false, length = 200)
    private String password;

    /** Nome completo do usuário (exibição no sistema) */
    @Column(length = 120)
    private String nomeCompleto;

    /**
     * Perfil de acesso.
     * @Enumerated(STRING) salva "ADMIN", "OPERADOR" etc. no banco
     * em vez de 0, 1, 2 — muito mais legível e seguro para migrations.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.OPERADOR;

    /** Desativar sem excluir (preserve histórico de auditoria) */
    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "ultimo_login")
    private LocalDateTime ultimoLogin;

    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    public void prePersist() {
        criadoEm = LocalDateTime.now();
    }

    // Helper para templates Thymeleaf
    public boolean isAdmin()      { return role == Role.ADMIN; }
    public boolean isOperador()   { return role == Role.OPERADOR || role == Role.ADMIN; }
    public boolean isVisualizador() { return role == Role.VISUALIZADOR; }
}
