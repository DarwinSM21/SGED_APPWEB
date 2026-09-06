package org.uteq.backend.academico.guardian.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.uteq.backend.academico.guardian.entity.GuardianStudent;

import java.util.List;
import java.util.Optional;

public interface GuardianStudentRepository extends JpaRepository<GuardianStudent, Long> {
    boolean existsByRepresentante_IdRepresentanteAndEstudiante_IdEstudianteAndActivoTrue(
            Long idRepresentante, Long idEstudiante);

    List<GuardianStudent> findByRepresentante_IdRepresentanteAndActivoTrue(Long idRepresentante);

    List<GuardianStudent> findByEstudiante_IdEstudianteAndActivoTrue(Long idEstudiante);

    Optional<GuardianStudent> findByRepresentante_IdRepresentanteAndEstudiante_IdEstudiante(
            Long idRepresentante, Long idEstudiante);

    @Procedure(procedureName = "academico.sp_contacto_representante_estudiante")
    String contactoDe(@Param("p_estudiante") Long idEstudiante);
}
