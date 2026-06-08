package com.sesmt.pgeo.repository;

import com.sesmt.pgeo.model.Agendamento;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    List<Agendamento> findByDataClinico(LocalDate dataClinico);

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
    @Query("""
        SELECT a FROM Agendamento a
        WHERE (:busca IS NULL OR LOWER(a.funcionarioNome) LIKE LOWER(CONCAT('%', :busca, '%')))
          AND (:dataInicio IS NULL OR a.dataClinico >= :dataInicio)
          AND (:dataFim IS NULL OR a.dataClinico <= :dataFim)
        ORDER BY a.dataClinico ASC, a.horaClinico ASC
        """)
    List<Agendamento> buscarComFiltros(@Param("busca") String busca,
                                       @Param("dataInicio") LocalDate dataInicio,
                                       @Param("dataFim") LocalDate dataFim);

    @Query("SELECT a FROM Agendamento a WHERE month(a.dataClinico) = :mes AND year(a.dataClinico) = :ano ORDER BY a.dataClinico ASC, a.horaClinico ASC")
    List<Agendamento> findByMesEAno(@Param("mes") int mes, @Param("ano") int ano);

    @Query(value = "SELECT DISTINCT EXTRACT(YEAR FROM data_clinico)::INTEGER FROM agendamento WHERE data_clinico IS NOT NULL ORDER BY 1 DESC", nativeQuery = true)
    List<Integer> findAnosDisponiveis();

    List<Agendamento> findAllByOrderByDataClinicoAsc();

    List<Agendamento> findByFuncionarioIdOrderByDataClinicoDesc(Long funcionarioId);
}
