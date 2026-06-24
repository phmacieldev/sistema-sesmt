/*
 * Copyright (c) 2026 Pedro Henrique Maciel da Silva Faria. Todos os direitos reservados.
 */
package com.sesmt.pgeo.dto;

import com.sesmt.pgeo.model.MedicalLeave;

public record AtestadoResponseDto(
    Long id,
    Long funcionarioId,
    String funcionarioNome,
    String dataAfastamento,
    Integer diasAfastamento,
    String tipo,
    String cid,
    String medicoNome,
    String medicoCrm
) {
    public static AtestadoResponseDto fromEntity(MedicalLeave ml) {
        return new AtestadoResponseDto(
            ml.getId(),
            ml.getFuncionario() != null ? ml.getFuncionario().getId() : null,
            ml.getFuncionario() != null ? ml.getFuncionario().getNome() : "",
            ml.getDataAfastamento() != null ? ml.getDataAfastamento().toString() : "",
            ml.getDiasAfastamento(),
            ml.getTipo() != null ? ml.getTipo().name() : "",
            ml.getCid() != null ? ml.getCid() : "",
            ml.getMedicoNome() != null ? ml.getMedicoNome() : "",
            ml.getMedicoCrm() != null ? ml.getMedicoCrm() : ""
        );
    }
}
