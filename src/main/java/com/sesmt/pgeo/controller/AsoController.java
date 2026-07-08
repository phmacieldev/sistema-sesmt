/*
 * Copyright (c) 2026 Pedro Henrique Maciel da Silva Faria. Todos os direitos reservados.
 * Desenvolvido de forma independente como projeto de portfólio.
 * Autorizado apenas para uso interno homologado.
 */
package com.sesmt.pgeo.controller;

import com.sesmt.pgeo.audit.AuditService;
import com.sesmt.pgeo.dto.AtualizarStatusAsoDto;
import com.sesmt.pgeo.exception.RegraDeNegocioException;
import com.sesmt.pgeo.service.AsoService;
import io.sentry.Sentry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@Tag(name = "ASO", description = "Atualização de status de envio e recebimento do ASO")
@RestController
@RequiredArgsConstructor
public class AsoController {

    private final AsoService asoService;
    private final AuditService auditService;

    @Operation(summary = "Atualiza status ASO", description = "Marca exame como enviado ou recebido. Campo: 'enviado' ou 'recebido'")
    @PostMapping("/atualizar_status_aso")
    public Map<String, Object> atualizarStatusAso(@Valid @RequestBody AtualizarStatusAsoDto dto) {
        try {
            asoService.atualizarStatusAso(dto.agendamento_id(), dto.campo(), dto.valor(),
                auditService.getUsuarioAtual());
            return Map.of("sucesso", true);
        } catch (RegraDeNegocioException e) {
            return Map.of("sucesso", false, "erro", e.getMessage());
        } catch (Exception e) {
            log.error("Erro ao atualizar status ASO do agendamento {}", dto.agendamento_id(), e);
            Sentry.captureException(e);
            return Map.of("sucesso", false, "erro", "Erro ao atualizar status do ASO.");
        }
    }
}
