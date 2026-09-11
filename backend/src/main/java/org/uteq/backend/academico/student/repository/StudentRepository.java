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

/**
 * Acceso a las fichas de estudiante, incluida la comprobación de duplicados
 * y los procedimientos almacenados de conteo, baja masiva, generación de
 * código y anonimización.
 */
public interface StudentRepository extends JpaRepository<Student, Long> {

    /**
     * @param pageable página y tamaño solicitados
     * @return página de estudiantes con baja lógica excluida
     */
    Page<Student> findByActivoTrue(Pageable pageable);

    /**
     * @return todos los estudiantes activos, ordenados por apellido, con
     *         persona y categoría precargadas para evitar N+1
     */
    @EntityGraph(attributePaths = {"persona", "categoria"})
    List<Student> findByActivoTrueOrderByPersona_ApellidoAsc();

    /**
     * @param idCategoria identificador de la categoría
     * @return la cantidad de estudiantes activos de esa categoría
     */
    long countByCategoria_IdCategoriaAndActivoTrue(Long idCategoria);

    /**
     * @param idEstudiante identificador del estudiante
     * @return el estudiante, si existe y está activo
     */
    Optional<Student> findByIdEstudianteAndActivoTrue(Long idEstudiante);

    /**
     * @param idPersona identificador de la persona
     * @return {@code true} si esa persona ya tiene una ficha de estudiante, activa o no
     */
    boolean existsByPersona_IdPersona(Long idPersona);

    /**
     * @param idPersona identificador de la persona
     * @return {@code true} si esa persona tiene una ficha de estudiante activa
     */
    boolean existsByPersona_IdPersonaAndActivoTrue(Long idPersona);

    /**
     * @param codigoEstudiante código único del estudiante
     * @return {@code true} si ya existe un estudiante con ese código
     */
    boolean existsByCodigoEstudiante(String codigoEstudiante);

    /**
     * @param idPersona identificador de la persona
     * @return la ficha de estudiante de esa persona, activa o no, si existe
     */
    Optional<Student> findByPersona_IdPersona(Long idPersona);

    /**
     * @param idPersona identificador de la persona
     * @return la ficha de estudiante activa de esa persona, si existe
     */
    Optional<Student> findByPersona_IdPersonaAndActivoTrue(Long idPersona);

    /**
     * Comprueba unicidad del código excluyendo al propio estudiante, para
     * permitir actualizar una ficha sin chocar consigo misma.
     *
     * @param codigoEstudiante código a comprobar
     * @param idEstudiante identificador del estudiante que se excluye de la comprobación
     * @return {@code true} si otro estudiante ya usa ese código
     */
    boolean existsByCodigoEstudianteAndIdEstudianteNot(String codigoEstudiante, Long idEstudiante);

    /**
     * @param username nombre de usuario de la cuenta de acceso
     * @return el estudiante cuya cuenta tiene ese nombre de usuario, si existe
     */
    Optional<Student> findByUsuario_Username(String username);

    /**
     * @param idCategoria identificador de la categoría
     * @param idEstudiante identificador del estudiante que se excluye del resultado
     * @return estudiantes activos de esa categoría, sin incluir al indicado
     */
    List<Student> findByCategoria_IdCategoriaAndActivoTrueAndIdEstudianteNot(Long idCategoria, Long idEstudiante);

    /**
     * @param idCategoria identificador de la categoría
     * @return estudiantes activos de esa categoría, ordenados por apellido
     */
    List<Student> findByCategoria_IdCategoriaAndActivoTrueOrderByPersona_ApellidoAsc(Long idCategoria);

    /**
     * @param idUsuario identificador de la cuenta de usuario
     * @return {@code true} si esa cuenta está vinculada a una ficha de estudiante
     */
    boolean existsByUsuario_IdUsuario(Long idUsuario);

    /**
     * @param idCategoria identificador de la categoría, o {@code null} para el total general
     * @return la cantidad de estudiantes activos, calculada por el procedimiento almacenado
     */
    @Procedure(procedureName = "academico.sp_contar_estudiantes_activos")
        Long countActiveStudentsByCategory(@Param("p_categoria") Long idCategoria);

    /**
     * Da de baja lógica a todos los estudiantes activos de una categoría de
     * una sola vez (usado al eliminar o vaciar una categoría del catálogo).
     *
     * @param idCategoria identificador de la categoría a vaciar
     */
    @Procedure(procedureName = "academico.sp_desactivar_estudiantes_categoria")
        void deactivateStudentsByCategory(@Param("p_categoria") Long idCategoria);

    /**
     * @param anio año que forma parte del código a generar
     * @return el siguiente código de estudiante disponible para ese año
     */
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

    /**
     * Consulta filtrada para el reporte PDF de estudiantes.
     *
     * @param idCategoria identificador de la categoría a filtrar, o {@code null} para todas
     * @param activo {@code true}/{@code false} para filtrar por estado, o {@code null} para ambos
     * @return estudiantes que cumplen el filtro, ordenados por apellido y nombre
     */
    @Query("""
           SELECT e FROM Student e
           WHERE (:idCategoria IS NULL OR e.categoria.idCategoria = :idCategoria)
             AND (:activo IS NULL OR e.activo = :activo)
           ORDER BY e.persona.apellido, e.persona.nombre
           """)
    List<Student> findForReport(@Param("idCategoria") Long idCategoria, @Param("activo") Boolean activo);
}
