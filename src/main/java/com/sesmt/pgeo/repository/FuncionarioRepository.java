package com.sesmt.pgeo.repository;

import com.sesmt.pgeo.model.Funcionario;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FuncionarioRepository extends JpaRepository<Funcionario, Long> {

    Optional<Funcionario> findByMatricula(String matricula);

    List<Funcionario> findByNomeContainingIgnoreCaseOrderByNomeAsc(String nome);

    // Fix: LIMIT → Pageable
    @Query("SELECT f FROM Funcionario f WHERE LOWER(f.nome) LIKE LOWER(CONCAT('%', :nome, '%')) ORDER BY f.nome")
    List<Funcionario> buscarPorNomeLimitado(@Param("nome") String nome, Pageable pageable);

    // Fix: LIMIT → Pageable
    @Query("SELECT f FROM Funcionario f ORDER BY f.id DESC")
    List<Funcionario> findUltimosCadastrados(Pageable pageable);

    default Optional<Funcionario> findUltimoCadastrado() {
        List<Funcionario> result = findUltimosCadastrados(PageRequest.of(0, 1));
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    /** Retorna apenas funcionários ativos, evitando carregar inativos desnecessariamente */
    List<Funcionario> findByAtivoTrue();
}
