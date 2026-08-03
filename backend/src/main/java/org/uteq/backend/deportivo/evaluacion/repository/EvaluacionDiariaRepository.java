package org.uteq.backend.deportivo.evaluacion.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.deportivo.evaluacion.entity.EvaluacionDiaria;

import java.util.Optional;

public interface EvaluacionDiariaRepository extends JpaRepository<EvaluacionDiaria, Long> {

    Optional<EvaluacionDiaria> findBySesionIdSesion(Long idSesion);

    boolean existsBySesionIdSesion(Long idSesion);
}
