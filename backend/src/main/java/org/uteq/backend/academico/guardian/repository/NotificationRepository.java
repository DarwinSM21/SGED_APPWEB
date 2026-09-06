package org.uteq.backend.academico.guardian.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.academico.guardian.entity.Notification;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRepresentante_IdRepresentanteOrderByCreatedAtDesc(Long idRepresentante);

    long countByRepresentante_IdRepresentanteAndLeidaFalse(Long idRepresentante);

    Optional<Notification> findByIdNotificacionAndRepresentante_IdRepresentante(
            Long idNotificacion, Long idRepresentante);
}
