package org.uteq.backend.deportivo.evaluation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.uteq.backend.deportivo.evaluation.entity.EvaluationCriterion;

import java.util.List;

public interface EvaluationCriterionRepository extends JpaRepository<EvaluationCriterion, Long> {

    @Query("SELECT c FROM EvaluationCriterion c WHERE c.activo = true ORDER BY c.idCriterio ASC")
    List<EvaluationCriterion> findActiveOrderByIdAsc();
}
