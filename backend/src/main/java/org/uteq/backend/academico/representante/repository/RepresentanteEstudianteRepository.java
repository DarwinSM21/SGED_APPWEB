package org.uteq.backend.academico.representante.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.uteq.backend.academico.representante.entity.RepresentanteEstudiante;

import java.util.List;
import java.util.Optional;

public interface RepresentanteEstudianteRepository extends JpaRepository<RepresentanteEstudiante, Long> {
    boolean existsByRepresentante_IdRepresentanteAndEstudiante_IdEstudianteAndActivoTrue(
            Long idRepresentante, Long idEstudiante);

    List<RepresentanteEstudiante> findByRepresentante_IdRepresentanteAndActivoTrue(Long idRepresentante);

    List<RepresentanteEstudiante> findByEstudiante_IdEstudianteAndActivoTrue(Long idEstudiante);

    Optional<RepresentanteEstudiante> findByRepresentante_IdRepresentanteAndEstudiante_IdEstudiante(
            Long idRepresentante, Long idEstudiante);

    @Procedure(procedureName = "academico.sp_contacto_representante_estudiante")
    String contactoDe(@Param("p_estudiante") Long idEstudiante);
}
