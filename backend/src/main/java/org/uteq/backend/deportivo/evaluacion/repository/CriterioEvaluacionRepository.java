package org.uteq.backend.deportivo.evaluacion.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.deportivo.evaluacion.entity.CriterioEvaluacion;

import java.util.List;

public interface CriterioEvaluacionRepository extends JpaRepository<CriterioEvaluacion, Long> {

    List<CriterioEvaluacion> findByActivoTrueOrderByIdCriterioAsc();
}
