package com.sesmt.pgeo.model.enums;

public enum TipoAtestado {
    DOENCA("Doença"),
    ACIDENTE_TRABALHO("Acidente de Trabalho"),
    NAO_INFORMADO("Não Informado");

    private final String descricao;

    TipoAtestado(String d) { this.descricao = d; }

    public String getDescricao() { return descricao; }
}
