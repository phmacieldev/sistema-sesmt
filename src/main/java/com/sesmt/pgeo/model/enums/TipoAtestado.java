/*
 * Copyright (c) 2026 Pedro Henrique Maciel da Silva Faria. Todos os direitos reservados.
 * Desenvolvido de forma independente como projeto de portfólio.
 * Autorizado apenas para uso interno homologado.
 */
package com.sesmt.pgeo.model.enums;

public enum TipoAtestado {
    DOENCA("Doença"),
    ACIDENTE_TRABALHO("Acidente de Trabalho"),
    NAO_INFORMADO("Não Informado");

    private final String descricao;

    TipoAtestado(String d) { this.descricao = d; }

    public String getDescricao() { return descricao; }
}
