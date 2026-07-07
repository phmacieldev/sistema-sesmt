/*
 * Copyright (c) 2026 Pedro Henrique Maciel da Silva Faria. Todos os direitos reservados.
 * Desenvolvido de forma independente como projeto de portfólio.
 * Autorizado apenas para uso interno homologado.
 */
package com.sesmt.pgeo.repository;

import com.sesmt.pgeo.model.Agendamento;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public interface AgendamentoRepository extends JpaRepository<Agendamento, Long>,
                                                JpaSpecificationExecutor<Agendamento> {

    List<Agendamento> findByDataClinico(LocalDate dataClinico);

    List<Agendamento> findByDataClinicoBetweenOrderByDataClinicoAsc(LocalDate inicio, LocalDate fim);

    List<Agendamento> findByDataSangueOrderByDataSangueAscHoraClinicoAsc(LocalDate dataSangue);

    // ── Duplicidade ───────────────────────────────────────────────────
    @Query("SELECT a FROM Agendamento a WHERE a.funcionarioMatricula = :matricula AND year(a.dataClinico) = :ano ORDER BY a.dataClinico DESC")
    List<Agendamento> findByMatriculaEAnoList(@Param("matricula") String matricula, @Param("ano") int ano, Pageable pageable);

    default Optional<Agendamento> findByMatriculaEAno(String matricula, int ano) {
        List<Agendamento> r = findByMatriculaEAnoList(matricula, ano, PageRequest.of(0, 1));
        return r.isEmpty() ? Optional.empty() : Optional.of(r.get(0));
    }

    @Query("SELECT a FROM Agendamento a WHERE LOWER(a.funcionarioNome) = LOWER(:nome) AND year(a.dataClinico) = :ano ORDER BY a.dataClinico DESC")
    List<Agendamento> findByNomeEAnoList(@Param("nome") String nome, @Param("ano") int ano, Pageable pageable);

    default Optional<Agendamento> findByNomeEAno(String nome, int ano) {
        List<Agendamento> r = findByNomeEAnoList(nome, ano, PageRequest.of(0, 1));
        return r.isEmpty() ? Optional.empty() : Optional.of(r.get(0));
    }

    // ── Limite de sangue: conta agendamentos com dataSangue num dia ───
    @Query("SELECT COUNT(a) FROM Agendamento a WHERE a.dataSangue = :data AND a.dataSangue IS NOT NULL")
    long countByDataSangue(@Param("data") LocalDate data);

    // ── Dashboard de sangue ───────────────────────────────────────────
    @Query("""
        SELECT a FROM Agendamento a
        WHERE a.dataSangue IS NOT NULL
          AND month(a.dataSangue) = :mes
          AND year(a.dataSangue)  = :ano
        ORDER BY a.dataSangue ASC
        """)
    List<Agendamento> findByMesEAnoSangue(@Param("mes") int mes, @Param("ano") int ano);

    // ── Filtros do dashboard principal ────────────────────────────────
    // Queries internas: :buscaLike já vem pré-processado (lowercase + %) do método default
    // Isso evita o bug lower(bytea) do PostgreSQL com parâmetros nulos sem tipo inferido.

    @Query("""
        SELECT a FROM Agendamento a
        WHERE (:busca IS NULL OR LOWER(a.funcionarioNome) LIKE :buscaLike)
          AND (:dataInicio IS NULL OR a.dataClinico >= :dataInicio)
          AND (:dataFim IS NULL OR a.dataClinico <= :dataFim)
        ORDER BY a.dataClinico ASC, a.horaClinico ASC
        """)
    List<Agendamento> buscarComFiltrosInternal(@Param("busca") String busca,
                                               @Param("buscaLike") String buscaLike,
                                               @Param("dataInicio") LocalDate dataInicio,
                                               @Param("dataFim") LocalDate dataFim);

    default List<Agendamento> buscarComFiltros(String busca, LocalDate dataInicio, LocalDate dataFim) {
        return buscarComFiltrosInternal(busca, like(busca), dataInicio, dataFim);
    }

    @Query("SELECT a FROM Agendamento a WHERE month(a.dataClinico) = :mes AND year(a.dataClinico) = :ano ORDER BY a.dataClinico ASC, a.horaClinico ASC")
    List<Agendamento> findByMesEAno(@Param("mes") int mes, @Param("ano") int ano);

    // ── Paginação server-side (dashboard principal) ───────────────────
    default Page<Agendamento> findByMesEAnoPaginado(int mes, int ano, String busca, String est, Pageable pageable) {
        String buscaLike = like(busca);
        String estUpper  = (est != null && !est.isBlank() && !"todos".equalsIgnoreCase(est)) ? est.toUpperCase() : null;
        java.time.YearMonth ym = java.time.YearMonth.of(ano, mes);
        LocalDate inicio = ym.atDay(1);
        LocalDate fim    = ym.atEndOfMonth();
        Specification<Agendamento> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.between(root.get("dataClinico"), inicio, fim));
            if (buscaLike != null)
                predicates.add(cb.like(cb.lower(root.get("funcionarioNome")), buscaLike));
            if (estUpper != null)
                predicates.add(cb.equal(cb.upper(root.join("funcionario").get("estabelecimento")), estUpper));
            if (query != null && !Long.class.isAssignableFrom(query.getResultType()))
                query.orderBy(cb.asc(root.get("dataClinico")), cb.asc(root.get("horaClinico")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return findAll(spec, pageable);
    }

    default Page<Agendamento> buscarPaginado(String busca, LocalDate dataInicio, LocalDate dataFim, String est, Pageable pageable) {
        String buscaLike = like(busca);
        String estUpper  = (est != null && !est.isBlank() && !"todos".equalsIgnoreCase(est)) ? est.toUpperCase() : null;
        Specification<Agendamento> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (buscaLike != null)
                predicates.add(cb.like(cb.lower(root.get("funcionarioNome")), buscaLike));
            if (dataInicio != null)
                predicates.add(cb.greaterThanOrEqualTo(root.get("dataClinico"), dataInicio));
            if (dataFim != null)
                predicates.add(cb.lessThanOrEqualTo(root.get("dataClinico"), dataFim));
            if (estUpper != null)
                predicates.add(cb.equal(cb.upper(root.join("funcionario").get("estabelecimento")), estUpper));
            if (query != null && !Long.class.isAssignableFrom(query.getResultType()))
                query.orderBy(cb.asc(root.get("dataClinico")), cb.asc(root.get("horaClinico")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return findAll(spec, pageable);
    }

    // ── Export sem paginação (mesmo filtro do dashboard, sem Pageable) ──
    @Query("""
        SELECT a FROM Agendamento a LEFT JOIN a.funcionario f
        WHERE month(a.dataClinico) = :mes AND year(a.dataClinico) = :ano
          AND (:busca IS NULL OR LOWER(a.funcionarioNome) LIKE :buscaLike)
          AND (:est IS NULL OR UPPER(f.estabelecimento) = :est)
        ORDER BY a.dataClinico ASC, a.horaClinico ASC
        """)
    List<Agendamento> findByMesEAnoExportInternal(
        @Param("mes") int mes, @Param("ano") int ano,
        @Param("busca") String busca, @Param("buscaLike") String buscaLike,
        @Param("est") String est);

    default List<Agendamento> findByMesEAnoExport(int mes, int ano, String busca, String est) {
        return findByMesEAnoExportInternal(mes, ano, busca, like(busca), est);
    }

    @Query("""
        SELECT a FROM Agendamento a LEFT JOIN a.funcionario f
        WHERE (:busca IS NULL OR LOWER(a.funcionarioNome) LIKE :buscaLike)
          AND (:dataInicio IS NULL OR a.dataClinico >= :dataInicio)
          AND (:dataFim IS NULL OR a.dataClinico <= :dataFim)
          AND (:est IS NULL OR UPPER(f.estabelecimento) = :est)
        ORDER BY a.dataClinico ASC, a.horaClinico ASC
        """)
    List<Agendamento> buscarTodosExportInternal(
        @Param("busca") String busca, @Param("buscaLike") String buscaLike,
        @Param("dataInicio") LocalDate dataInicio,
        @Param("dataFim") LocalDate dataFim,
        @Param("est") String est);

    default List<Agendamento> buscarTodosExport(String busca, LocalDate dataInicio, LocalDate dataFim, String est) {
        return buscarTodosExportInternal(busca, like(busca), dataInicio, dataFim, est);
    }

    /** Converte o termo de busca para o padrão LIKE em minúsculas. */
    private static String like(String busca) {
        return busca != null ? "%" + busca.toLowerCase() + "%" : null;
    }

    @Query(value = "SELECT DISTINCT EXTRACT(YEAR FROM data_clinico)::INTEGER FROM agendamento WHERE data_clinico IS NOT NULL ORDER BY 1 DESC", nativeQuery = true)
    List<Integer> findAnosDisponiveis();

    List<Agendamento> findAllByOrderByDataClinicoAsc();

    List<Agendamento> findByFuncionarioIdOrderByDataClinicoDesc(Long funcionarioId);
}
