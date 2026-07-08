/*
 * Copyright (c) 2026 Pedro Henrique Maciel da Silva Faria. Todos os direitos reservados.
 */
package com.sesmt.pgeo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record UpdateAgendamentoDto(
    @NotBlank(message = "Nome é obrigatório") String nome,
    String setor,
    String funcao,
    @NotBlank(message = "Tipo de exame é obrigatório") String tipo_exame,
    @NotNull(message = "Data do exame clínico é obrigatória")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data_clinico,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data_sangue,
    @NotBlank(message = "Horário é obrigatório") String hora,
    String observacoes,
    String exames_sangue
) {}
