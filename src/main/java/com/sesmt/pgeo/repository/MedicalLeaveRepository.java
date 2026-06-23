/*
 * Copyright (c) 2026 Pedro Henrique Maciel da Silva Faria. Todos os direitos reservados.
 * Desenvolvido de forma independente como projeto de portfólio.
 * Autorizado apenas para uso interno homologado.
 */
package com.sesmt.pgeo.repository;

import com.sesmt.pgeo.model.MedicalLeave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MedicalLeaveRepository extends JpaRepository<MedicalLeave, Long> {

    @Query("SELECT ml FROM MedicalLeave ml WHERE ml.dataAfastamento >= :limite ORDER BY ml.dataAfastamento DESC")
    List<MedicalLeave> findRecentes(@Param("limite") LocalDate limite);

    @Query("SELECT ml FROM MedicalLeave ml JOIN FETCH ml.funcionario f WHERE ml.dataAfastamento BETWEEN :inicio AND :fim ORDER BY f.nome ASC, ml.dataAfastamento ASC")
    List<MedicalLeave> findBySemana(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    @Query("SELECT ml FROM MedicalLeave ml JOIN FETCH ml.funcionario f WHERE ml.dataAfastamento BETWEEN :inicio AND :fim ORDER BY ml.id ASC")
    List<MedicalLeave> findBySemanaOrdemLancamento(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    @Query("SELECT ml FROM MedicalLeave ml JOIN FETCH ml.funcionario f WHERE ml.dataAfastamento BETWEEN :inicio AND :fim AND LOWER(f.nome) LIKE LOWER(CONCAT('%',:nome,'%')) ORDER BY ml.id ASC")
    List<MedicalLeave> findBySemanaENome(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim, @Param("nome") String nome);

    @Query("SELECT ml FROM MedicalLeave ml JOIN FETCH ml.funcionario f WHERE LOWER(f.nome) LIKE LOWER(CONCAT('%',:nome,'%')) ORDER BY ml.dataAfastamento DESC, ml.id DESC")
    List<MedicalLeave> findByNomeTodos(@Param("nome") String nome);

    @Query("SELECT ml FROM MedicalLeave ml JOIN FETCH ml.funcionario f WHERE f.id = :id ORDER BY ml.dataAfastamento DESC")
    List<MedicalLeave> findByFuncionarioIdOrderByDataDesc(@Param("id") Long id);

    @Query("SELECT ml FROM MedicalLeave ml JOIN FETCH ml.funcionario f WHERE ml.dataAfastamento >= :limite ORDER BY ml.dataAfastamento DESC")
    List<MedicalLeave> findUltimos60Dias(@Param("limite") LocalDate limite);

    List<MedicalLeave> findByFuncionarioIdOrderByDataAfastamentoDesc(Long funcionarioId);

    @Query("SELECT MAX(ml.dataAfastamento) FROM MedicalLeave ml")
    java.util.Optional<LocalDate> findDataMaisRecente();

    @Query("SELECT ml FROM MedicalLeave ml WHERE ml.dataAfastamento >= :inicio ORDER BY ml.dataAfastamento ASC")
    List<MedicalLeave> findDesde(@Param("inicio") LocalDate inicio);
}
