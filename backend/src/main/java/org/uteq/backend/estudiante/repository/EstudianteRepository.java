package org.uteq.backend.estudiante.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.uteq.backend.estudiante.entity.Estudiante;

/**
 * Estrategia híbrida de acceso a datos (Bloque A.2):
 * - CRUD elementales -> métodos derivados de Spring Data (ORM).
 * - Agregados y actualizaciones masivas -> funciones/procedimientos
 *   almacenados versionados en db/procs/, invocados con @Procedure
 *   conforme a JPA 2.1. Parámetros siempre nombrados; prohibida la
 *   concatenación de entrada de usuario.
 */
public interface EstudianteRepository extends JpaRepository<Estudiante, Long> {

    Page<Estudiante> findByActivoTrue(Pageable pageable);

    /** fn_contar_estudiantes_activos: COUNT con filtro (agregado -> SQL en el motor). */
    @Procedure(procedureName = "seguridad.fn_contar_estudiantes_activos")
    long contarActivosPorCategoria(@Param("p_categoria") String categoria);

    /** fn_desactivar_estudiantes_categoria: UPDATE masivo con criterio de negocio. */
    @Procedure(procedureName = "seguridad.fn_desactivar_estudiantes_categoria")
    int desactivarPorCategoria(@Param("p_categoria") String categoria);
}
