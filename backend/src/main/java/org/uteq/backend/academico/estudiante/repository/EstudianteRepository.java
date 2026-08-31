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

    @EntityGraph(attributePaths = {"persona", "categoria"})
    List<Estudiante> findByActivoTrueOrderByPersona_ApellidoAsc();

    long countByCategoria_IdCategoriaAndActivoTrue(Long idCategoria);

    Optional<Estudiante> findByIdEstudianteAndActivoTrue(Long idEstudiante);

    boolean existsByPersona_IdPersona(Long idPersona);

    boolean existsByPersona_IdPersonaAndActivoTrue(Long idPersona);

    boolean existsByCodigoEstudiante(String codigoEstudiante);

    Optional<Estudiante> findByPersona_IdPersona(Long idPersona);

    Optional<Estudiante> findByPersona_IdPersonaAndActivoTrue(Long idPersona);

    boolean existsByCodigoEstudianteAndIdEstudianteNot(String codigoEstudiante, Long idEstudiante);

    Optional<Estudiante> findByUsuario_Username(String username);

    List<Estudiante> findByCategoria_IdCategoriaAndActivoTrueAndIdEstudianteNot(Long idCategoria, Long idEstudiante);

    List<Estudiante> findByCategoria_IdCategoriaAndActivoTrueOrderByPersona_ApellidoAsc(Long idCategoria);

    boolean existsByUsuario_IdUsuario(Long idUsuario);

    @Procedure(procedureName = "academico.sp_contar_estudiantes_activos")
        Long contarEstudiantesActivosPorCategoria(@Param("p_categoria") Long idCategoria);

    @Procedure(procedureName = "academico.sp_desactivar_estudiantes_categoria")
        void desactivarEstudiantesPorCategoria(@Param("p_categoria") Long idCategoria);

    @Procedure(procedureName = "academico.sp_generar_codigo_estudiante")
        String generarSiguienteCodigo(@Param("p_anio") Integer anio);

    @Query("""
           SELECT e FROM Estudiante e
           WHERE (:idCategoria IS NULL OR e.categoria.idCategoria = :idCategoria)
             AND (:activo IS NULL OR e.activo = :activo)
           ORDER BY e.persona.apellido, e.persona.nombre
           """)
    List<Estudiante> buscarParaReporte(@Param("idCategoria") Long idCategoria, @Param("activo") Boolean activo);
}
