package org.uteq.backend.deportivo.sesion.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.deportivo.sesion.entity.SesionEntrenamiento;

import java.time.LocalDate;
import java.util.List;

public interface SesionEntrenamientoRepository extends JpaRepository<SesionEntrenamiento, Long> {

    List<SesionEntrenamiento> findByFechaOrderByHoraInicioAsc(LocalDate fecha);

    Page<SesionEntrenamiento> findByEntrenadorIdEntrenadorOrderByFechaDesc(Long idEntrenador, Pageable pageable);

    /**
     * Sesion anterior de la misma categoria. Es la que alimenta la precarga:
     * cada dia arranca con los valores del entrenamiento previo, para que el
     * entrenador ajuste lo que cambio en vez de calificar todo desde cero.
     */
    List<SesionEntrenamiento> findByCategoriaIdCategoriaAndFechaLessThanOrderByFechaDesc(
            Long idCategoria, LocalDate fecha, Pageable pageable);
}
