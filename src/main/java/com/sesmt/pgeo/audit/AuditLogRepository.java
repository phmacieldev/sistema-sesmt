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
}
