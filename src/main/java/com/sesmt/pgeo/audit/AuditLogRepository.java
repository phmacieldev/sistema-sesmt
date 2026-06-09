package com.sesmt.pgeo.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    // Últimas N ações de um usuário
    List<AuditLog> findByUsuarioOrderByCriadoEmDesc(String usuario, Pageable pageable);

    // Histórico de um registro específico
    List<AuditLog> findByEntidadeAndEntidadeIdOrderByCriadoEmDesc(String entidade, Long id);

    // Ações num intervalo de datas — para relatório de auditoria
    @Query("SELECT a FROM AuditLog a WHERE a.criadoEm BETWEEN :inicio AND :fim ORDER BY a.criadoEm DESC")
    Page<AuditLog> findByPeriodo(
        @Param("inicio") LocalDateTime inicio,
        @Param("fim") LocalDateTime fim,
        Pageable pageable);

    // Busca com todos os filtros opcionais — usado pela tela de auditoria admin
    @Query("""
        SELECT a FROM AuditLog a
        WHERE (:usuario IS NULL OR LOWER(a.usuario) LIKE LOWER(CONCAT('%', :usuario, '%')))
          AND (:entidade IS NULL OR a.entidade = :entidade)
          AND (:acao IS NULL OR a.acao = :acao)
          AND (:inicio IS NULL OR a.criadoEm >= :inicio)
          AND (:fim IS NULL OR a.criadoEm <= :fim)
        ORDER BY a.criadoEm DESC
        """)
    Page<AuditLog> buscarComFiltros(
        @Param("usuario")  String usuario,
        @Param("entidade") String entidade,
        @Param("acao")     String acao,
        @Param("inicio")   LocalDateTime inicio,
        @Param("fim")      LocalDateTime fim,
        Pageable pageable);

    @Query("SELECT DISTINCT a.entidade FROM AuditLog a ORDER BY a.entidade ASC")
    List<String> findDistinctEntidades();

    @Query("SELECT DISTINCT a.acao FROM AuditLog a ORDER BY a.acao ASC")
    List<String> findDistinctAcoes();
}
