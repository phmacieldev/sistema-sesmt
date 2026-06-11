/*
 * Copyright (c) 2026 Pedro Henrique Maciel da Silva Faria. Todos os direitos reservados.
 * Desenvolvido de forma independente como projeto de portfólio.
 * Autorizado apenas para uso interno homologado.
 */
package com.sesmt.pgeo.repository;

import com.sesmt.pgeo.model.HistoricoCargo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistoricoCargoRepository extends JpaRepository<HistoricoCargo, Long> {
    List<HistoricoCargo> findByFuncionarioIdOrderByAlteradoEmDesc(Long funcionarioId);
}
