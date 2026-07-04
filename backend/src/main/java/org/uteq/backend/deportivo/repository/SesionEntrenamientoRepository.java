package org.uteq.backend.deportivo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.uteq.backend.deportivo.entity.SesionEntrenamiento;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SesionEntrenamientoRepository extends JpaRepository<SesionEntrenamiento, Long> {

    Page<SesionEntrenamiento> findByFecha(LocalDate fecha, Pageable pageable);

    Page<SesionEntrenamiento> findByEntrenadorIdEntrenador(Long idEntrenador, Pageable pageable);

    @Query("SELECT s FROM SesionEntrenamiento s WHERE s.entrenador.idEntrenador = :idEntrenador AND s.fecha = :fecha")
    List<SesionEntrenamiento> findByEntrenadorAndFecha(
            @Param("idEntrenador") Long idEntrenador,
            @Param("fecha") LocalDate fecha);

    @Query("SELECT s FROM SesionEntrenamiento s WHERE s.entrenador.idEntrenador = :idEntrenador " +
           "ORDER BY s.fecha DESC LIMIT 1")
    SesionEntrenamiento findLatestByEntrenador(@Param("idEntrenador") Long idEntrenador);
}
