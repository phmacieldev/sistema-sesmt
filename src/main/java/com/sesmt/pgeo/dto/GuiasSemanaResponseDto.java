/*
 * Copyright (c) 2026 Pedro Henrique Maciel da Silva Faria. Todos os direitos reservados.
 */
package com.sesmt.pgeo.dto;

import java.util.List;

public record GuiasSemanaResponseDto(
    String periodoSangue,
    String periodoClinico,
    List<GuiaSangueDto> sangue,
    List<GuiaClinicoDto> clinico
) {
    public record GuiaSangueDto(Long id, String nome, String setor, String data, String hora, String exames) {}
    public record GuiaClinicoDto(Long id, String nome, String setor, String data, String hora, String tipo) {}
}
