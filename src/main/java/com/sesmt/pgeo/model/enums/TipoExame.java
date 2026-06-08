package com.sesmt.pgeo.model.enums;

/**
 * Tipos de exame ocupacional (NR-7).
 *
 * Usar enum em vez de String livre evita erros de digitação
 * e facilita comparações no código (sem .equals("Periódico") espalhado).
 */
public enum TipoExame {
    PERIODICO("Periódico"),
    ADMISSIONAL("Admissional"),
    DEMISSIONAL("Demissional"),
    RETORNO_AO_TRABALHO("Retorno ao Trabalho"),
    MUDANCA_DE_RISCO("Mudança de Risco");

    private final String descricao;

    TipoExame(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    /** Converte a String do formulário para o enum (tolerante a variações) */
    public static TipoExame fromDescricao(String descricao) {
        if (descricao == null) return null;
        for (TipoExame t : values()) {
            if (t.descricao.equalsIgnoreCase(descricao.strip())) return t;
        }
        return null;
    }

    @Override
    public String toString() {
        return descricao;
    }
}
