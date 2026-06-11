/*
 * Copyright (c) 2026 Pedro Henrique Maciel da Silva Faria. Todos os direitos reservados.
 * Desenvolvido de forma independente como projeto de portfólio.
 * Autorizado apenas para uso interno homologado.
 */
package com.sesmt.pgeo.audit;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>,
                                             JpaSpecificationExecutor<AuditLog> {

    // Últimas N ações de um usuário
    List<AuditLog> findByUsuarioOrderByCriadoEmDesc(String usuario, Pageable pageable);

    // Histórico de um registro específico
    List<AuditLog> findByEntidadeAndEntidadeIdOrderByCriadoEmDesc(String entidade, Long id);

    @Query("SELECT DISTINCT a.entidade FROM AuditLog a ORDER BY a.entidade ASC")
    List<String> findDistinctEntidades();

    @Query("SELECT DISTINCT a.acao FROM AuditLog a ORDER BY a.acao ASC")
    List<String> findDistinctAcoes();

    // Busca paginada com filtros opcionais via Specification (Hibernate 6 safe)
    default Page<AuditLog> buscarComFiltros(String usuario, String entidade, String acao,
                                             LocalDateTime inicio, LocalDateTime fim,
                                             Pageable pageable) {
        Specification<AuditLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (usuario != null && !usuario.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("usuario")),
                                       "%" + usuario.toLowerCase() + "%"));
            }
            if (entidade != null && !entidade.isBlank()) {
                predicates.add(cb.equal(root.get("entidade"), entidade));
            }
            if (acao != null && !acao.isBlank()) {
                predicates.add(cb.equal(root.get("acao"), acao));
            }
            if (inicio != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("criadoEm"), inicio));
            }
            if (fim != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("criadoEm"), fim));
            }
            // Só adiciona orderBy na query de dados, não na count query
            if (query != null && !Long.class.isAssignableFrom(query.getResultType())) {
                query.orderBy(cb.desc(root.get("criadoEm")));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return findAll(spec, pageable);
    }
}
