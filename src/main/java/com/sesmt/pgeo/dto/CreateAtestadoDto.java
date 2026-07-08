/*
 * Copyright (c) 2026 Pedro Henrique Maciel da Silva Faria. Todos os direitos reservados.
 */
package com.sesmt.pgeo.dto;

import com.sesmt.pgeo.model.enums.TipoAtestado;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record CreateAtestadoDto(
    @NotNull(message = "Funcionário é obrigatório") Long funcionarioId,
    @NotNull(message = "Data do atestado é obrigatória")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataAfastamento,
    @NotNull(message = "Dias de afastamento é obrigatório")
    @Min(value = 1, message = "Mínimo 1 dia")
    @Max(value = 365, message = "Máximo 365 dias") Integer diasAfastamento,
    @NotNull(message = "Tipo é obrigatório") TipoAtestado tipo,
    String cid,
    String medicoNome,
    String medicoCrm
) {}
