package org.uteq.backend.academico.representante.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.academico.representante.entity.Notificacion;

import java.util.List;
import java.util.Optional;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
    List<Notificacion> findByRepresentante_IdRepresentanteOrderByCreatedAtDesc(Long idRepresentante);

    long countByRepresentante_IdRepresentanteAndLeidaFalse(Long idRepresentante);

    Optional<Notificacion> findByIdNotificacionAndRepresentante_IdRepresentante(
            Long idNotificacion, Long idRepresentante);
}
