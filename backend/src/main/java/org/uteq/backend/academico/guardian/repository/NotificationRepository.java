package org.uteq.backend.academico.guardian.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
    @Query("SELECT n FROM Notification n WHERE n.guardian.id = :idRepresentante ORDER BY n.createdAt DESC")
    List<Notification> findByGuardian_IdOrderByCreatedAtDesc(@Param("idRepresentante") Long idRepresentante);

    /**
     * @param idRepresentante identificador del representante
     * @return la cantidad de notificaciones sin leer de ese representante
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.guardian.id = :idRepresentante AND n.read = false")
    long countByGuardian_IdAndReadFalse(@Param("idRepresentante") Long idRepresentante);

    /**
     * Busca una notificación puntual y comprueba a la vez que pertenece al
     * representante, para no permitir marcar como leída una notificación ajena.
     *
     * @param idNotificacion identificador de la notificación
     * @param idRepresentante identificador del representante que la solicita
     * @return la notificación, si existe y pertenece a ese representante
     */
    @Query("SELECT n FROM Notification n WHERE n.id = :idNotificacion AND n.guardian.id = :idRepresentante")
    Optional<Notification> findByIdAndGuardian_Id(
            @Param("idNotificacion") Long idNotificacion, @Param("idRepresentante") Long idRepresentante);
}
