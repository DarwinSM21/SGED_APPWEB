package org.uteq.backend.academico.estudianteRepresentante.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.academico.estudianteRepresentante.entity.EstudianteRepresentante;

import java.util.List;

public interface EstudianteRepresentanteRepository extends JpaRepository<EstudianteRepresentante, Long> {

    List<EstudianteRepresentante> findByEstudiante_IdEstudiante(Long idEstudiante);

    boolean existsByEstudiante_IdEstudianteAndRepresentante_IdRepresentante(Long idEstudiante, Long idRepresentante);
}
