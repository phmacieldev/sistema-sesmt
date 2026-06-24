/*
 * Copyright (c) 2026 Pedro Henrique Maciel da Silva Faria. Todos os direitos reservados.
 */
package com.sesmt.pgeo.dto;

import com.sesmt.pgeo.model.Agendamento;
import com.sesmt.pgeo.model.enums.TipoExame;

import java.time.LocalDate;

public record CalendarioEventoDto(
    Long id,
    String title,
    String start,
    String color,
    ExtendedProps extendedProps
) {
    public record ExtendedProps(
        String nome, String setor, String funcao, String tipo,
        LocalDate sangue, boolean exigeSangue
    ) {}

    public static CalendarioEventoDto fromEntity(Agendamento a) {
        String cor = switch (a.getTipoExame() != null ? a.getTipoExame() : TipoExame.PERIODICO) {
            case PERIODICO           -> "#27ae60";
            case ADMISSIONAL         -> "#2980b9";
            case DEMISSIONAL         -> "#e74c3c";
            case RETORNO_AO_TRABALHO -> "#f1c40f";
            case MUDANCA_DE_RISCO    -> "#ff8800";
        };

        String primeiroNome = a.getFuncionarioNome() != null
            ? a.getFuncionarioNome().split(" ")[0] : "?";

        return new CalendarioEventoDto(
            a.getId(),
            primeiroNome + " | " + a.getTipoExameDescricao(),
            a.getDataClinico() + "T" + a.getHoraClinico(),
            cor,
            new ExtendedProps(
                a.getFuncionarioNome(),
                a.getFuncionarioSetor() != null ? a.getFuncionarioSetor() : "",
                a.getFuncionarioFuncao() != null ? a.getFuncionarioFuncao() : "",
                a.getTipoExameDescricao(),
                a.getDataSangue(),
                a.isExigeSangue()
            )
        );
    }
}
