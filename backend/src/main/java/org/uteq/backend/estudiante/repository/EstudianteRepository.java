package org.uteq.backend.estudiante.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.uteq.backend.estudiante.entity.Estudiante;

@Repository
public interface EstudianteRepository extends JpaRepository<Estudiante, Long> {

    Page<Estudiante> findByActivoTrue(Pageable pageable);

    /**
     * Agregado COUNT (Bloque A.2.2): obligatoriamente via procedimiento
     * almacenado, no JPQL. Ver db/procs/sp_contar_estudiantes_activos.sql.
     */
    @Procedure(procedureName = "seguridad.sp_contar_estudiantes_activos")
    long contarActivosPorCategoria(@Param("p_categoria") String categoria);

    /**
     * Baja logica masiva con criterio de negocio (Bloque A.2.2): update
     * multi-fila via procedimiento almacenado. Ver
     * db/procs/sp_desactivar_estudiantes_categoria.sql.
     */
    @Procedure(procedureName = "seguridad.sp_desactivar_estudiantes_categoria")
    int desactivarPorCategoria(@Param("p_categoria") String categoria);
}
