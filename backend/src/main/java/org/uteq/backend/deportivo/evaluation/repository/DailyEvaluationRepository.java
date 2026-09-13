package org.uteq.backend.deportivo.evaluation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.uteq.backend.deportivo.evaluation.entity.DailyEvaluation;

import java.util.Optional;

/**
 * Acceso a la evaluación diaria de una sesión de entrenamiento (cabecera que
 * agrupa los puntajes por criterio de cada jugador evaluado).
 */
public interface DailyEvaluationRepository extends JpaRepository<DailyEvaluation, Long> {

    /**
     * @param idSesion identificador de la sesión de entrenamiento
     * @return la evaluación diaria de esa sesión, si existe
     */
    @Query("SELECT ed FROM DailyEvaluation ed WHERE ed.sesion.idSesion = :idSesion")
    Optional<DailyEvaluation> findBySession_Id(@Param("idSesion") Long idSesion);

    /**
     * @param idSesion identificador de la sesión de entrenamiento
     * @return {@code true} si esa sesión ya tiene una evaluación diaria registrada
     */
    @Query("SELECT COUNT(ed) > 0 FROM DailyEvaluation ed WHERE ed.sesion.idSesion = :idSesion")
    boolean existsBySession_Id(@Param("idSesion") Long idSesion);
}
