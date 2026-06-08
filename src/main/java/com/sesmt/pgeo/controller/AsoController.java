package com.sesmt.pgeo.controller;

import com.sesmt.pgeo.audit.AuditService;
import com.sesmt.pgeo.service.AsoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AsoController {

    private final AsoService asoService;
    private final AuditService auditService;

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
            return Map.of("sucesso", false, "erro", e.getMessage());
        }
    }
}
