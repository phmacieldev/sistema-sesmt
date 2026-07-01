/*
 * Copyright (c) 2026 Pedro Henrique Maciel da Silva Faria. Todos os direitos reservados.
 * Desenvolvido de forma independente como projeto de portfólio.
 * Autorizado apenas para uso interno homologado.
 */
package com.sesmt.pgeo.service;

import com.sesmt.pgeo.audit.AuditService;
import com.sesmt.pgeo.dto.CreateAtestadoDto;
import com.sesmt.pgeo.dto.UpdateAtestadoDto;
import com.sesmt.pgeo.exception.RecursoNaoEncontradoException;
import com.sesmt.pgeo.model.MedicalLeave;
import com.sesmt.pgeo.model.enums.TipoAtestado;
import com.sesmt.pgeo.repository.FuncionarioRepository;
import com.sesmt.pgeo.repository.MedicalLeaveRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AtestadoService {

    private final MedicalLeaveRepository repo;
    private final FuncionarioRepository  funcRepo;
    private final AuditService           auditService;

    // ── CRUD ──────────────────────────────────────────────────────────

    @Transactional
    public MedicalLeave criar(CreateAtestadoDto dto) {
        MedicalLeave ml = new MedicalLeave();
        preencherCampos(ml, dto.funcionarioId(), dto.dataAfastamento(),
            dto.diasAfastamento(), dto.cid(), dto.medicoNome(), dto.medicoCrm(), dto.tipo());
        MedicalLeave salvo = repo.save(ml);
        auditService.registrarCriacao("Atestado", salvo.getId(),
            "Atestado de " + salvo.getFuncionario().getNome() + " (" + salvo.getDiasAfastamento() + " dias)");
        return salvo;
    }

    @Transactional
    public MedicalLeave criar(Long funcionarioId, LocalDate dataAfastamento,
                               Integer diasAfastamento, String motivo, String cid,
                               String medicoNome, String medicoCrm, TipoAtestado tipo) {
        MedicalLeave ml = new MedicalLeave();
        preencherCampos(ml, funcionarioId, dataAfastamento, diasAfastamento,
            cid, medicoNome, medicoCrm, tipo);
        ml.setMotivo(motivo);
        MedicalLeave salvo = repo.save(ml);
        auditService.registrarCriacao("Atestado", salvo.getId(),
            "Atestado de " + salvo.getFuncionario().getNome() + " (" + salvo.getDiasAfastamento() + " dias)");
        return salvo;
    }

    @Transactional
    public MedicalLeave editar(Long id, UpdateAtestadoDto dto) {
        MedicalLeave ml = buscarPorId(id);
        String antes = descricaoAuditoria(ml);
        preencherCampos(ml, dto.funcionarioId(), dto.dataAfastamento(),
            dto.diasAfastamento(), dto.cid(), dto.medicoNome(), dto.medicoCrm(), dto.tipo());
        MedicalLeave salvo = repo.save(ml);
        auditService.registrarEdicao("Atestado", id,
            "Edição de atestado de " + salvo.getFuncionario().getNome(), antes, descricaoAuditoria(salvo));
        return salvo;
    }

    @Transactional
    public MedicalLeave editar(Long id, Long funcionarioId, LocalDate dataAfastamento,
                                Integer diasAfastamento, String motivo, String cid,
                                String medicoNome, String medicoCrm, TipoAtestado tipo) {
        MedicalLeave ml = buscarPorId(id);
        String antes = descricaoAuditoria(ml);
        preencherCampos(ml, funcionarioId, dataAfastamento, diasAfastamento,
            cid, medicoNome, medicoCrm, tipo);
        ml.setMotivo(motivo);
        MedicalLeave salvo = repo.save(ml);
        auditService.registrarEdicao("Atestado", id,
            "Edição de atestado de " + salvo.getFuncionario().getNome(), antes, descricaoAuditoria(salvo));
        return salvo;
    }

    @Transactional
    public void excluir(Long id) {
        MedicalLeave ml = buscarPorId(id);
        String desc = descricaoAuditoria(ml);
        String nome = ml.getFuncionario().getNome();
        repo.delete(ml);
        auditService.registrarExclusao("Atestado", id, "Exclusão de atestado de " + nome, desc);
    }

    private String descricaoAuditoria(MedicalLeave ml) {
        return ml.getDataAfastamento() + " | " + ml.getDiasAfastamento() + " dias | " + ml.getCid();
    }

    public MedicalLeave buscarPorId(Long id) {
        return repo.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Atestado", id));
    }

    private void preencherCampos(MedicalLeave ml, Long funcionarioId, LocalDate dataAfastamento,
                                  Integer diasAfastamento, String cid, String medicoNome,
                                  String medicoCrm, TipoAtestado tipo) {
        ml.setFuncionario(funcRepo.findById(funcionarioId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Funcionario", funcionarioId)));
        ml.setDataAfastamento(dataAfastamento);
        ml.setDiasAfastamento(diasAfastamento);
        ml.setCid(cid != null ? cid.toUpperCase().strip() : null);
        ml.setMedicoNome(medicoNome != null ? medicoNome.strip() : null);
        ml.setMedicoCrm(medicoCrm != null ? medicoCrm.strip() : null);
        ml.setTipo(tipo);
    }

    // ── Semana e estatísticas ─────────────────────────────────────────

    /** Encontra a terça-feira que inicia a semana do dia fornecido. */
    public LocalDate semanaInicio(LocalDate referencia) {
        int daysBack = (referencia.getDayOfWeek().getValue() - DayOfWeek.TUESDAY.getValue() + 7) % 7;
        return referencia.minusDays(daysBack);
    }

    public LocalDate semanaFim(LocalDate semanaInicio) {
        return semanaInicio.plusDays(6);
    }

    /** Agrupa dias de afastamento por funcionário nos últimos 60 dias. Marca >= 15 dias como risco INSS. */
    @Data
    public static class ResumoFuncionario {
        private final Long funcionarioId;
        private final String nome;
        private final String setor;
        private final int totalDias;
        private final int totalAtestados;
        private final List<MedicalLeave> atestados;
        public boolean isRiscoInss() { return totalDias >= 15; }
    }

    public List<ResumoFuncionario> resumoPor60Dias(List<MedicalLeave> atestados) {
        return atestados.stream()
            .collect(Collectors.groupingBy(ml -> ml.getFuncionario().getId()))
            .values().stream()
            .map(lista -> {
                MedicalLeave primeiro = lista.get(0);
                int totalDias = lista.stream().mapToInt(MedicalLeave::getDiasAfastamento).sum();
                return new ResumoFuncionario(
                    primeiro.getFuncionario().getId(),
                    primeiro.getFuncionario().getNome(),
                    primeiro.getFuncionario().getSetor(),
                    totalDias,
                    lista.size(),
                    lista
                );
            })
            .sorted(Comparator.comparingInt(ResumoFuncionario::getTotalDias).reversed())
            .collect(Collectors.toList());
    }

    /** Totais por setor: Map<setor, totalDias> */
    public Map<String, Integer> totalPorSetor(List<MedicalLeave> lista) {
        return lista.stream().collect(Collectors.groupingBy(
            ml -> ml.getFuncionario().getSetor() != null ? ml.getFuncionario().getSetor() : "Não informado",
            TreeMap::new,
            Collectors.summingInt(MedicalLeave::getDiasAfastamento)
        ));
    }

    /** Totais por tipo: Map<descricao, count> */
    public Map<String, Long> totalPorTipo(List<MedicalLeave> lista) {
        return lista.stream().collect(Collectors.groupingBy(
            ml -> ml.getTipo() != null ? ml.getTipo().getDescricao() : "Não Informado",
            TreeMap::new,
            Collectors.counting()
        ));
    }
}
