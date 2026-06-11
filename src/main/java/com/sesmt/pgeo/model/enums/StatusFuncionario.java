/*
 * Copyright (c) 2026 Pedro Henrique Maciel da Silva Faria. Todos os direitos reservados.
 * Desenvolvido de forma independente como projeto de portfólio.
 * Autorizado apenas para uso interno homologado.
 */
package com.sesmt.pgeo.model.enums;

public enum StatusFuncionario {
    ATIVO("Ativo"),
    PRE_ADMISSIONAL("Pré-admissional"),   // agendado antes de entrar no sistema da empresa
    DESLIGADO("Desligado");

    private final String descricao;
    StatusFuncionario(String descricao) { this.descricao = descricao; }
    public String getDescricao() { return descricao; }
}
