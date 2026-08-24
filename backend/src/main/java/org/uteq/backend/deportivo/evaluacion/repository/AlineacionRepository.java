package org.uteq.backend.deportivo.evaluacion.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.deportivo.evaluacion.entity.Alineacion;

import java.util.Optional;

public interface AlineacionRepository extends JpaRepository<Alineacion, Long> {

    /**
     * La alineacion guardada de una sesion, si la hay. El EntityGraph evita el
     * N+1 al pintar el once: sin el, cada jugador dispara su propia consulta
     * de estudiante, persona y posicion.
     */
    @EntityGraph(attributePaths = {
            "jugadores", "jugadores.estudiante", "jugadores.estudiante.persona", "jugadores.posicion"})
    Optional<Alineacion> findBySesion_IdSesion(Long idSesion);

    boolean existsBySesion_IdSesion(Long idSesion);
}
