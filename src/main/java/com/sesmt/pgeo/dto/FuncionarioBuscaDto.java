/*
 * Copyright (c) 2026 Pedro Henrique Maciel da Silva Faria. Todos os direitos reservados.
 */
package com.sesmt.pgeo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sesmt.pgeo.model.Funcionario;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FuncionarioBuscaDto(
    boolean encontrado,
    String matricula,
    String nome,
    String setor,
    String funcao,
    Boolean exigeSangue,
    Boolean preAdmissional,
    String estabelecimento
) {
    public static FuncionarioBuscaDto encontrado(Funcionario f) {
        return new FuncionarioBuscaDto(
            true,
            f.getMatricula(),
            f.getNome(),
            f.getSetor(),
            f.getFuncao(),
            f.isExigeSangue(),
            f.isPreAdmissional(),
            f.getEstabelecimentoEfetivo()
        );
    }

    public static FuncionarioBuscaDto naoEncontrado() {
        return new FuncionarioBuscaDto(false, null, null, null, null, null, null, null);
    }
}
