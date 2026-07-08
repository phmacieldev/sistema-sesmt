/*
 * Copyright (c) 2026 Pedro Henrique Maciel da Silva Faria. Todos os direitos reservados.
 * Desenvolvido de forma independente como projeto de portfólio.
 * Autorizado apenas para uso interno homologado.
 */
package com.sesmt.pgeo.service;

import com.sesmt.pgeo.audit.AuditService;
import com.sesmt.pgeo.dto.CreateAtestadoDto;
import com.sesmt.pgeo.model.Funcionario;
import com.sesmt.pgeo.model.MedicalLeave;
import com.sesmt.pgeo.model.enums.TipoAtestado;
import com.sesmt.pgeo.repository.FuncionarioRepository;
import com.sesmt.pgeo.repository.MedicalLeaveRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Cobre a trilha de auditoria do CRUD de atestados — antes desta correção,
 * AtestadoService não chamava AuditService em nenhum ponto.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AtestadoServiceCrudTest {

    @Mock MedicalLeaveRepository repo;
    @Mock FuncionarioRepository  funcRepo;
    @Mock AuditService           auditService;

    @InjectMocks AtestadoService service;

    @Test
    void criar_registraAuditoriaDeCriacao() {
        Funcionario func = new Funcionario();
        func.setId(1L);
        func.setNome("Ana");

        when(funcRepo.findById(1L)).thenReturn(Optional.of(func));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateAtestadoDto dto = new CreateAtestadoDto(
            1L, LocalDate.now(), 3, TipoAtestado.DOENCA, "J11", "Dr. Fulano", "CRM 123");

        service.criar(dto);

        verify(auditService).registrarCriacao(eq("Atestado"), any(), any());
    }

    @Test
    void excluir_registraAuditoriaDeExclusao() {
        Funcionario func = new Funcionario();
        func.setId(1L);
        func.setNome("Ana");

        MedicalLeave ml = new MedicalLeave();
        ml.setId(9L);
        ml.setFuncionario(func);
        ml.setDataAfastamento(LocalDate.now());
        ml.setDiasAfastamento(2);

        when(repo.findById(9L)).thenReturn(Optional.of(ml));

        service.excluir(9L);

        verify(repo).delete(ml);
        verify(auditService).registrarExclusao(eq("Atestado"), eq(9L), any(), any());
    }
}
