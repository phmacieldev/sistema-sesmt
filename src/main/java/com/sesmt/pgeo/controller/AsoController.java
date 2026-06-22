/*
 * Copyright (c) 2026 Pedro Henrique Maciel da Silva Faria. Todos os direitos reservados.
 * Desenvolvido de forma independente como projeto de portfólio.
 * Autorizado apenas para uso interno homologado.
 */
package com.sesmt.pgeo.controller;

import com.sesmt.pgeo.audit.AuditService;
import com.sesmt.pgeo.service.AsoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "ASO", description = "Atualização de status de envio e recebimento do ASO")
@RestController
@RequiredArgsConstructor
public class AsoController {

    private final AsoService asoService;
    private final AuditService auditService;

    @Operation(summary = "Atualiza status ASO", description = "Marca exame como enviado ou recebido. Campo: 'enviado' ou 'recebido'")
    @PostMapping("/atualizar_status_aso")
    public Map<String, Object> atualizarStatusAso(@RequestBody Map<String, Object> dados) {
        try {
            Long agId   = Long.valueOf(dados.get("agendamento_id").toString());
            String campo = (String) dados.get("campo");
            boolean valor = Boolean.parseBoolean(dados.get("valor").toString());
            String usuario = auditService.getUsuarioAtual();

            asoService.atualizarStatusAso(agId, campo, valor, usuario);
            return Map.of("sucesso", true);

        } catch (Exception e) {
            return Map.of("sucesso", false, "erro", "Erro ao atualizar status do ASO.");
        }
    }
}
