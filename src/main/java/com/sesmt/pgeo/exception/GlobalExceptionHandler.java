/*
 * Copyright (c) 2026 Pedro Henrique Maciel da Silva Faria. Todos os direitos reservados.
 * Desenvolvido de forma independente como projeto de portfólio.
 * Autorizado apenas para uso interno homologado.
 */
package com.sesmt.pgeo.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    /** Retorna true se a requisição espera JSON (fetch/AJAX) */
    private boolean isJsonRequest(HttpServletRequest req) {
        String accept = req.getHeader("Accept");
        String contentType = req.getHeader("Content-Type");
        return (accept != null && accept.contains("application/json"))
            || (contentType != null && contentType.contains("application/json"))
            || "XMLHttpRequest".equals(req.getHeader("X-Requested-With"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public Object handleNoResource(NoResourceFoundException ex,
                                   Model model, HttpServletRequest req) {
        if (isJsonRequest(req)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("erro", true, "mensagem", "Recurso não encontrado."));
        }
        model.addAttribute("titulo", "Não encontrado");
        model.addAttribute("mensagem", "O recurso solicitado não existe.");
        model.addAttribute("status", 404);
        return "error/erro";
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public Object handleNaoEncontrado(RecursoNaoEncontradoException ex,
                                      Model model, HttpServletRequest req) {
        log.warn("Recurso não encontrado [{}]: {}", req.getRequestURI(), ex.getMessage());
        String msgCliente = "O recurso solicitado não foi encontrado.";
        if (isJsonRequest(req)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("erro", true, "mensagem", msgCliente));
        }
        model.addAttribute("titulo", "Não encontrado");
        model.addAttribute("mensagem", msgCliente);
        model.addAttribute("status", 404);
        return "error/erro";
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public Object handleRegraDeNegocio(RegraDeNegocioException ex,
                                       Model model, HttpServletRequest req) {
        log.warn("Regra de negócio [{}]: {}", req.getRequestURI(), ex.getMessage());
        if (isJsonRequest(req)) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("erro", true, "mensagem", ex.getMessage()));
        }
        model.addAttribute("titulo", "Operação não permitida");
        model.addAttribute("mensagem", ex.getMessage());
        model.addAttribute("status", 422);
        return "error/erro";
    }

    @ExceptionHandler({OptimisticLockException.class, ObjectOptimisticLockingFailureException.class})
    public Object handleConflito(Exception ex, Model model, HttpServletRequest req) {
        log.warn("Conflito de edição simultânea [{}]", req.getRequestURI());
        String msg = "Este registro foi alterado por outro usuário enquanto você editava. Recarregue a página e tente novamente.";
        if (isJsonRequest(req)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("erro", true, "mensagem", msg));
        }
        model.addAttribute("titulo", "Conflito de edição");
        model.addAttribute("mensagem", msg);
        model.addAttribute("status", 409);
        return "error/erro";
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Object handleAcessoNegado(AccessDeniedException ex,
                                     Model model, HttpServletRequest req) {
        log.warn("Acesso negado [{}]", req.getRequestURI());
        if (isJsonRequest(req)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("erro", true, "mensagem", "Acesso negado."));
        }
        model.addAttribute("titulo", "Acesso negado");
        model.addAttribute("mensagem", "Você não tem permissão para realizar esta ação.");
        model.addAttribute("status", 403);
        return "error/erro";
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object handleValidacao(MethodArgumentNotValidException ex,
                                   Model model, HttpServletRequest req) {
        String mensagens = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getDefaultMessage())
            .collect(Collectors.joining("; "));
        log.warn("Validação falhou [{}]: {}", req.getRequestURI(), mensagens);
        if (isJsonRequest(req)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("ok", false, "mensagem", mensagens));
        }
        model.addAttribute("titulo", "Dados inválidos");
        model.addAttribute("mensagem", mensagens);
        model.addAttribute("status", 400);
        return "error/erro";
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public Object handleBadRequest(Exception ex, Model model, HttpServletRequest req) {
        log.warn("Parâmetro inválido [{}]: {}", req.getRequestURI(), ex.getMessage());
        String msg = "Parâmetro inválido na requisição.";
        if (isJsonRequest(req)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("erro", true, "mensagem", msg));
        }
        model.addAttribute("titulo", "Requisição inválida");
        model.addAttribute("mensagem", msg);
        model.addAttribute("status", 400);
        return "error/erro";
    }

    @ExceptionHandler(Exception.class)
    public Object handleGenerico(Exception ex, Model model, HttpServletRequest req) {
        log.error("Erro interno [{}]: {}", req.getRequestURI(), ex.getMessage(), ex);
        if (isJsonRequest(req)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("erro", true, "mensagem", "Erro interno do servidor."));
        }
        model.addAttribute("titulo", "Erro interno");
        model.addAttribute("mensagem",
            "Ocorreu um erro inesperado. O suporte foi notificado. Tente novamente em instantes.");
        model.addAttribute("status", 500);
        return "error/erro";
    }
}
