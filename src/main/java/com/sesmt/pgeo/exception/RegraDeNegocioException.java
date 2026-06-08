package com.sesmt.pgeo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Lançada quando uma regra de negócio é violada (horário ocupado, duplicidade, etc.) */
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class RegraDeNegocioException extends RuntimeException {
    public RegraDeNegocioException(String mensagem) {
        super(mensagem);
    }
}
