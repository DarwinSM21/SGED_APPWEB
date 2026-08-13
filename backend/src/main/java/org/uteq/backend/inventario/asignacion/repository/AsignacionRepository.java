package org.uteq.backend.inventario.asignacion.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.inventario.asignacion.entity.Asignacion;

public interface AsignacionRepository extends JpaRepository<Asignacion, Long> {

    Page<Asignacion> findAllByOrderByFechaAsignacionDesc(Pageable pageable);

    Page<Asignacion> findByEstudiante_IdEstudianteOrderByFechaAsignacionDesc(Long idEstudiante, Pageable pageable);

    Page<Asignacion> findByEntrenador_IdEntrenadorOrderByFechaAsignacionDesc(Long idEntrenador, Pageable pageable);
}
