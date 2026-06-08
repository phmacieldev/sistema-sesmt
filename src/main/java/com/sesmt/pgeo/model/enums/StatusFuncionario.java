package com.sesmt.pgeo.model.enums;

public enum StatusFuncionario {
    ATIVO("Ativo"),
    PRE_ADMISSIONAL("Pré-admissional"),   // agendado antes de entrar no sistema da empresa
    DESLIGADO("Desligado");

    private final String descricao;
    StatusFuncionario(String descricao) { this.descricao = descricao; }
    public String getDescricao() { return descricao; }
}
