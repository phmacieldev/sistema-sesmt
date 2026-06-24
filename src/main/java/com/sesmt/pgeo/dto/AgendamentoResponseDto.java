/*
 * Copyright (c) 2026 Pedro Henrique Maciel da Silva Faria. Todos os direitos reservados.
 */
package com.sesmt.pgeo.dto;

import com.sesmt.pgeo.model.Agendamento;

public record AgendamentoResponseDto(
    Long id,
    String matricula,
    String nome,
    String setor,
    String funcao,
    String tipoExame,
    String dataSangue,
    String dataClinico,
    String hora,
    String observacoes,
    String examesSangue
) {
    public static AgendamentoResponseDto fromEntity(Agendamento ag) {
        return new AgendamentoResponseDto(
            ag.getId(),
            ag.getFuncionarioMatricula() != null ? ag.getFuncionarioMatricula() : "",
            ag.getFuncionarioNome(),
            ag.getFuncionarioSetor(),
            ag.getFuncionarioFuncao(),
            ag.getTipoExameDescricao(),
            ag.getDataSangue() != null ? ag.getDataSangue().toString() : "",
            ag.getDataClinico() != null ? ag.getDataClinico().toString() : "",
            ag.getHoraClinico() != null ? ag.getHoraClinico() : "",
            ag.getObservacoes() != null ? ag.getObservacoes() : "",
            ag.getExamesSangue() != null ? ag.getExamesSangue() : ""
        );
    }
}
