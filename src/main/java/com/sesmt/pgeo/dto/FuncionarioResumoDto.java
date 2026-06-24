/*
 * Copyright (c) 2026 Pedro Henrique Maciel da Silva Faria. Todos os direitos reservados.
 */
package com.sesmt.pgeo.dto;

import com.sesmt.pgeo.model.Funcionario;

public record FuncionarioResumoDto(
    Long id,
    String matricula,
    String nome,
    String setor,
    String funcao,
    boolean exigeSangue
) {
    public static FuncionarioResumoDto fromEntity(Funcionario f) {
        return new FuncionarioResumoDto(
            f.getId(),
            f.getMatricula(),
            f.getNome(),
            f.getSetor(),
            f.getFuncao(),
            f.isExigeSangue()
        );
    }
}
