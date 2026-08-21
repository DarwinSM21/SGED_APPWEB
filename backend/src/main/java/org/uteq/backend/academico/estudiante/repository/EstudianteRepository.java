package org.uteq.backend.academico.estudiante.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.uteq.backend.academico.estudiante.entity.Estudiante;

import java.util.List;
import java.util.Optional;

public interface EstudianteRepository extends JpaRepository<Estudiante, Long> {

    Page<Estudiante> findByActivoTrue(Pageable pageable);

    /** Padron completo de activos, para los paneles que recorren a todos y no paginan. */
    /**
     * Trae persona y categoria en la misma consulta. Sin el EntityGraph son
     * LAZY, y quien recorre la lista para leer el nombre dispara una consulta
     * por estudiante: medido con 2.008 alumnos, el panel de alertas tardaba
     * 1,4 s de los cuales solo 72 ms eran la consulta de asistencia; el resto
     * era este N+1 escondido detras de un getter.
     */
    @EntityGraph(attributePaths = {"persona", "categoria"})
    List<Estudiante> findByActivoTrueOrderByPersona_ApellidoAsc();

    long countByCategoria_IdCategoriaAndActivoTrue(Long idCategoria);

    Optional<Estudiante> findByIdEstudianteAndActivoTrue(Long idEstudiante);

    boolean existsByPersona_IdPersona(Long idPersona);

    /** Coherencia rol-ficha: solo una ficha vigente condiciona el rol de la cuenta. */
    boolean existsByPersona_IdPersonaAndActivoTrue(Long idPersona);

    boolean existsByCodigoEstudiante(String codigoEstudiante);

    Optional<Estudiante> findByPersona_IdPersona(Long idPersona);

    /** Vincula una cuenta nueva a una ficha que ya existia (ver UsuarioService.crear). */
    Optional<Estudiante> findByPersona_IdPersonaAndActivoTrue(Long idPersona);

    boolean existsByCodigoEstudianteAndIdEstudianteNot(String codigoEstudiante, Long idEstudiante);

    /** Resuelve el estudiante a partir del usuario autenticado (JWT -> username). */
    Optional<Estudiante> findByUsuario_Username(String username);

    /** Companeros de equipo: los demas estudiantes activos de la misma categoria. */
    List<Estudiante> findByCategoria_IdCategoriaAndActivoTrueAndIdEstudianteNot(Long idCategoria, Long idEstudiante);

    /** Todos los estudiantes activos de una categoria (evaluacion diaria: se listan todos, no solo quien marco asistencia). */
    List<Estudiante> findByCategoria_IdCategoriaAndActivoTrueOrderByPersona_ApellidoAsc(Long idCategoria);

    boolean existsByUsuario_IdUsuario(Long idUsuario);

    @Procedure(procedureName = "academico.sp_contar_estudiantes_activos")
        Long contarEstudiantesActivosPorCategoria(@Param("p_categoria") Long idCategoria);

    @Procedure(procedureName = "academico.sp_desactivar_estudiantes_categoria")
        void desactivarEstudiantesPorCategoria(@Param("p_categoria") Long idCategoria);

    /** Propone el siguiente codigo_estudiante consecutivo del anio dado (formato EST-<anio>-0000). */
    @Procedure(procedureName = "academico.sp_generar_codigo_estudiante")
        String generarSiguienteCodigo(@Param("p_anio") Integer anio);

    /** Reporte de fichas: filtros opcionales de categoria y estado activo. */
    @Query("""
           SELECT e FROM Estudiante e
           WHERE (:idCategoria IS NULL OR e.categoria.idCategoria = :idCategoria)
             AND (:activo IS NULL OR e.activo = :activo)
           ORDER BY e.persona.apellido, e.persona.nombre
           """)
    List<Estudiante> buscarParaReporte(@Param("idCategoria") Long idCategoria, @Param("activo") Boolean activo);
}