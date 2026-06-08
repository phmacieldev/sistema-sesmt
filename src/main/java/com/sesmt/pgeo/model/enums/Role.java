package com.sesmt.pgeo.model.enums;

/**
 * Perfis de acesso do sistema.
 *
 * ADMIN      → acesso total, gerencia usuários, exclui registros
 * OPERADOR   → cria e edita agendamentos, lança atestados
 * VISUALIZADOR → somente leitura (relatórios, calendário)
 */
public enum Role {
    ADMIN,
    OPERADOR,
    VISUALIZADOR
}
