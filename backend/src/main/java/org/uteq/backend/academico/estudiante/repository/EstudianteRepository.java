package org.uteq.backend.academico.estudiante.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.academico.estudiante.entity.Estudiante;

import java.util.Optional;

public interface EstudianteRepository extends JpaRepository<Estudiante, Long> {

    Page<Estudiante> findByActivoTrue(Pageable pageable);

    long countByCategoria_IdCategoriaAndActivoTrue(Long idCategoria);

    Optional<Estudiante> findByIdEstudianteAndActivoTrue(Long idEstudiante);

    boolean existsByPersona_IdPersona(Long idPersona);

    boolean existsByCodigoEstudiante(String codigoEstudiante);

    Optional<Estudiante> findByPersona_IdPersona(Long idPersona);

    boolean existsByCodigoEstudianteAndIdEstudianteNot(String codigoEstudiante, Long idEstudiante);
}