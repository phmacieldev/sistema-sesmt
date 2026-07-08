/*
 * Copyright (c) 2026 Pedro Henrique Maciel da Silva Faria. Todos os direitos reservados.
 */
package com.sesmt.pgeo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AtualizarStatusAsoDto(
    @NotNull(message = "ID do agendamento é obrigatório") Long agendamento_id,
    @NotBlank(message = "Campo é obrigatório") String campo,
    @NotNull(message = "Valor é obrigatório") Boolean valor
) {}
