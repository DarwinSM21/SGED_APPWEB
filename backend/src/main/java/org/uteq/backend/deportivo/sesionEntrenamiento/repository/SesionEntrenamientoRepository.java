package org.uteq.backend.deportivo.sesionEntrenamiento.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.deportivo.sesionEntrenamiento.entity.SesionEntrenamiento;

public interface SesionEntrenamientoRepository extends JpaRepository<SesionEntrenamiento, Long> {
}
