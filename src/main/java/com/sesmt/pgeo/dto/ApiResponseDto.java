/*
 * Copyright (c) 2026 Pedro Henrique Maciel da Silva Faria. Todos os direitos reservados.
 */
package com.sesmt.pgeo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponseDto(
    boolean ok,
    String mensagem,
    Long id,
    Boolean duplicado
) {
    public static ApiResponseDto sucesso() {
        return new ApiResponseDto(true, null, null, null);
    }

    public static ApiResponseDto sucesso(Long id) {
        return new ApiResponseDto(true, null, id, null);
    }

    public static ApiResponseDto erro(String mensagem) {
        return new ApiResponseDto(false, mensagem, null, null);
    }

    public static ApiResponseDto duplicado(Long id, String mensagem) {
        return new ApiResponseDto(false, mensagem, id, true);
    }
}
