package org.uteq.backend.academico.guardian.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
    @Query("SELECT COUNT(v) > 0 FROM GuardianStudent v WHERE v.guardian.id = :idRepresentante AND v.student.id = :idEstudiante AND v.active = true")
    boolean existsByGuardian_IdAndStudent_IdAndActiveTrue(
            @Param("idRepresentante") Long idRepresentante, @Param("idEstudiante") Long idEstudiante);

    /**
     * @param idRepresentante identificador del representante
     * @return los estudiantes activos vinculados a ese representante
     */
    @Query("SELECT v FROM GuardianStudent v WHERE v.guardian.id = :idRepresentante AND v.active = true")
    List<GuardianStudent> findByGuardian_IdAndActiveTrue(@Param("idRepresentante") Long idRepresentante);

    /**
     * @param idEstudiante identificador del estudiante
     * @return los representantes activos vinculados a ese estudiante
     */
    @Query("SELECT v FROM GuardianStudent v WHERE v.student.id = :idEstudiante AND v.active = true")
    List<GuardianStudent> findByStudent_IdAndActiveTrue(@Param("idEstudiante") Long idEstudiante);

    /**
     * @param idRepresentante identificador del representante
     * @param idEstudiante identificador del estudiante
     * @return el vínculo entre ambos, activo o no, si existe
     */
    @Query("SELECT v FROM GuardianStudent v WHERE v.guardian.id = :idRepresentante AND v.student.id = :idEstudiante")
    Optional<GuardianStudent> findByGuardian_IdAndStudent_Id(
            @Param("idRepresentante") Long idRepresentante, @Param("idEstudiante") Long idEstudiante);

    /**
     * Invoca el procedimiento almacenado que resuelve el dato de contacto
     * (correo o teléfono) del representante activo de un estudiante.
     *
     * @param idEstudiante identificador del estudiante
     * @return el dato de contacto resuelto por el procedimiento, o {@code null}
     *         si el estudiante no tiene representante activo
     */
    @Procedure(procedureName = "academico.sp_contacto_representante_estudiante")
    String contactOf(@Param("p_estudiante") Long idEstudiante);
}
