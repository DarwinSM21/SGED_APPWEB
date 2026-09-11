package org.uteq.backend.academico.guardian.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.uteq.backend.academico.guardian.entity.GuardianStudent;

import java.util.List;
import java.util.Optional;

/**
 * Acceso al vínculo (muchos a muchos) entre representantes y estudiantes.
 */
public interface GuardianStudentRepository extends JpaRepository<GuardianStudent, Long> {

    /**
     * @param idRepresentante identificador del representante
     * @param idEstudiante identificador del estudiante
     * @return {@code true} si el vínculo entre ambos existe y está activo
     */
    boolean existsByRepresentante_IdRepresentanteAndEstudiante_IdEstudianteAndActivoTrue(
            Long idRepresentante, Long idEstudiante);

    /**
     * @param idRepresentante identificador del representante
     * @return los estudiantes activos vinculados a ese representante
     */
    List<GuardianStudent> findByRepresentante_IdRepresentanteAndActivoTrue(Long idRepresentante);

    /**
     * @param idEstudiante identificador del estudiante
     * @return los representantes activos vinculados a ese estudiante
     */
    List<GuardianStudent> findByEstudiante_IdEstudianteAndActivoTrue(Long idEstudiante);

    /**
     * @param idRepresentante identificador del representante
     * @param idEstudiante identificador del estudiante
     * @return el vínculo entre ambos, activo o no, si existe
     */
    Optional<GuardianStudent> findByRepresentante_IdRepresentanteAndEstudiante_IdEstudiante(
            Long idRepresentante, Long idEstudiante);

    /**
     * Invoca el procedimiento almacenado que resuelve el dato de contacto
     * (correo o teléfono) del representante activo de un estudiante.
     *
     * @param idEstudiante identificador del estudiante
     * @return el dato de contacto resuelto por el procedimiento, o {@code null}
     *         si el estudiante no tiene representante activo
     */
    @Procedure(procedureName = "academico.sp_contacto_representante_estudiante")
    String contactoDe(@Param("p_estudiante") Long idEstudiante);
}
