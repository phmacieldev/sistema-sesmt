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

    // Atestados dos últimos 60 dias, agrupados por funcionário
    // Equivalente à query do dashboard_routes.py
    @Query("SELECT ml FROM MedicalLeave ml WHERE ml.dataAfastamento >= :limite ORDER BY ml.dataAfastamento DESC")
    List<MedicalLeave> findRecentes(@Param("limite") LocalDate limite);
}
