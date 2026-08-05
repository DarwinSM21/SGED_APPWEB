package org.uteq.backend.deportivo.estadoAsistencia.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.deportivo.estadoAsistencia.entity.EstadoAsistencia;

public interface EstadoAsistenciaRepository extends JpaRepository<EstadoAsistencia, Long> {
}
