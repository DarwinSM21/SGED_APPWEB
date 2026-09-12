package org.uteq.backend.deportivo.evaluation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.uteq.backend.deportivo.evaluation.entity.DailyEvaluation;

import java.util.Optional;

public interface DailyEvaluationRepository extends JpaRepository<DailyEvaluation, Long> {

    @Query("SELECT ed FROM DailyEvaluation ed WHERE ed.sesion.idSesion = :idSesion")
    Optional<DailyEvaluation> findBySession_Id(@Param("idSesion") Long idSesion);

    @Query("SELECT COUNT(ed) > 0 FROM DailyEvaluation ed WHERE ed.sesion.idSesion = :idSesion")
    boolean existsBySession_Id(@Param("idSesion") Long idSesion);
}
