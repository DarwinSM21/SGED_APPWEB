package org.uteq.backend.academico.asistencia.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.academico.asistencia.entity.Asistencia;

import java.util.List;

public interface AsistenciaRepository extends JpaRepository<Asistencia, Long> {

    List<Asistencia> findByEstudiante_IdEstudiante(Long idEstudiante);

    List<Asistencia> findBySesionEntrenamiento_IdSesionEntrenamiento(Long idSesionEntrenamiento);
}
