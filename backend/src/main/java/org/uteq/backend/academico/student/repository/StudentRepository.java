package org.uteq.backend.academico.student.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.uteq.backend.academico.student.entity.Student;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {
    Page<Student> findByActivoTrue(Pageable pageable);

    @EntityGraph(attributePaths = {"persona", "categoria"})
    List<Student> findByActivoTrueOrderByPersona_ApellidoAsc();

    long countByCategoria_IdCategoriaAndActivoTrue(Long idCategoria);

    Optional<Student> findByIdEstudianteAndActivoTrue(Long idEstudiante);

    boolean existsByPersona_IdPersona(Long idPersona);

    boolean existsByPersona_IdPersonaAndActivoTrue(Long idPersona);

    boolean existsByCodigoEstudiante(String codigoEstudiante);

    Optional<Student> findByPersona_IdPersona(Long idPersona);

    Optional<Student> findByPersona_IdPersonaAndActivoTrue(Long idPersona);

    boolean existsByCodigoEstudianteAndIdEstudianteNot(String codigoEstudiante, Long idEstudiante);

    Optional<Student> findByUsuario_Username(String username);

    List<Student> findByCategoria_IdCategoriaAndActivoTrueAndIdEstudianteNot(Long idCategoria, Long idEstudiante);

    List<Student> findByCategoria_IdCategoriaAndActivoTrueOrderByPersona_ApellidoAsc(Long idCategoria);

    boolean existsByUsuario_IdUsuario(Long idUsuario);

    @Procedure(procedureName = "academico.sp_contar_estudiantes_activos")
        Long countActiveStudentsByCategory(@Param("p_categoria") Long idCategoria);

    @Procedure(procedureName = "academico.sp_desactivar_estudiantes_categoria")
        void deactivateStudentsByCategory(@Param("p_categoria") Long idCategoria);

    @Procedure(procedureName = "academico.sp_generar_codigo_estudiante")
        String generateNextCode(@Param("p_anio") Integer anio);

    /**
     * RF-50 / hallazgo H-03: anonimiza los datos identificativos de la persona
     * del estudiante y borra el texto libre escrito sobre el menor,
     * conservando las claves foráneas y las estadísticas agregadas. Delega en
     * el procedimiento almacenado versionado {@code sp_anonimizar_estudiante}
     * (migración {@code V27}).
     *
     * @param idEstudiante identificador del estudiante a anonimizar
     * @return número de filas tocadas por el procedimiento
     */
    @Procedure(procedureName = "academico.sp_anonimizar_estudiante")
        Integer anonymizeStudent(@Param("p_id_estudiante") Long idEstudiante);

    @Query("""
           SELECT e FROM Student e
           WHERE (:idCategoria IS NULL OR e.categoria.idCategoria = :idCategoria)
             AND (:activo IS NULL OR e.activo = :activo)
           ORDER BY e.persona.apellido, e.persona.nombre
           """)
    List<Student> findForReport(@Param("idCategoria") Long idCategoria, @Param("activo") Boolean activo);
}
