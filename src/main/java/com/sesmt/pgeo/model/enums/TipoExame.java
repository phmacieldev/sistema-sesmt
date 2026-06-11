package com.sesmt.pgeo.model.enums;

/**
 * Tipos de exame ocupacional (NR-7).
 *
 * Usar enum em vez de String livre evita erros de digitação
 * e facilita comparações no código (sem .equals("Periódico") espalhado).
 */
public enum TipoExame {
    PERIODICO("Periódico", "exame-periodico"),
    ADMISSIONAL("Admissional", "exame-admissional"),
    DEMISSIONAL("Demissional", "exame-demissional"),
    RETORNO_AO_TRABALHO("Retorno ao Trabalho", "exame-retorno"),
    MUDANCA_DE_RISCO("Mudança de Risco", "exame-mudanca");

    private final String descricao;
    private final String cssClass;

    TipoExame(String descricao, String cssClass) {
        this.descricao = descricao;
        this.cssClass  = cssClass;
    }

    public String getDescricao() { return descricao; }
    public String getCssClass()  { return cssClass; }

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
