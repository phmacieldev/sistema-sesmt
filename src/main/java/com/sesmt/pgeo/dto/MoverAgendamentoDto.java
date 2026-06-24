/*
 * Copyright (c) 2026 Pedro Henrique Maciel da Silva Faria. Todos os direitos reservados.
 */
package com.sesmt.pgeo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MoverAgendamentoDto(
    @NotNull(message = "ID é obrigatório") Long id,
    @NotBlank(message = "Data é obrigatória") String data,
    @NotBlank(message = "Hora é obrigatória") String hora
) {}
