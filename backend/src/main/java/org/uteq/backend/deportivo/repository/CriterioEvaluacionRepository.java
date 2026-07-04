package org.uteq.backend.deportivo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.uteq.backend.deportivo.entity.CriterioEvaluacion;

import java.util.List;
import java.util.Optional;

@Repository
public interface CriterioEvaluacionRepository extends JpaRepository<CriterioEvaluacion, Long> {

    List<CriterioEvaluacion> findByActivoTrue();

    Optional<CriterioEvaluacion> findByNombre(String nombre);
}
