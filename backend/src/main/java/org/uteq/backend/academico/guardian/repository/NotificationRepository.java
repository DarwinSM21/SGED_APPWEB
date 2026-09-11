package org.uteq.backend.academico.guardian.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.uteq.backend.academico.guardian.entity.Notification;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a las notificaciones en-app dirigidas a un representante.
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * @param idRepresentante identificador del representante
     * @return las notificaciones de ese representante, de la más reciente a la más antigua
     */
    List<Notification> findByRepresentante_IdRepresentanteOrderByCreatedAtDesc(Long idRepresentante);

    /**
     * @param idRepresentante identificador del representante
     * @return la cantidad de notificaciones sin leer de ese representante
     */
    long countByRepresentante_IdRepresentanteAndLeidaFalse(Long idRepresentante);

    /**
     * Busca una notificación puntual y comprueba a la vez que pertenece al
     * representante, para no permitir marcar como leída una notificación ajena.
     *
     * @param idNotificacion identificador de la notificación
     * @param idRepresentante identificador del representante que la solicita
     * @return la notificación, si existe y pertenece a ese representante
     */
    Optional<Notification> findByIdNotificacionAndRepresentante_IdRepresentante(
            Long idNotificacion, Long idRepresentante);
}
