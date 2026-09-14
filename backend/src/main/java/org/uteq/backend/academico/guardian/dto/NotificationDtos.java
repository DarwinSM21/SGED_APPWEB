package org.uteq.backend.academico.guardian.dto;

import org.uteq.backend.academico.guardian.entity.Notification.Type;

import java.time.Instant;

/** Contenedor de los DTO de notificaciones a representantes. */
public final class NotificationDtos {

    private NotificationDtos() {}

    /**
     * Vista de una notificación para el representante.
     *
     * @param notificationId identificador de la notificación
     * @param studentId      identificador del estudiante al que se refiere
     * @param student        nombre del estudiante al que se refiere
     * @param type           tipo de evento que la origina
     * @param message        texto de la notificación
     * @param read           {@code true} si el representante ya la marcó como leída
     * @param createdAt      fecha y hora de creación
     */
    public record NotificationResponse(
            Long notificationId,
            Long studentId,
            String student,
            Type type,
            String message,
            boolean read,
            Instant createdAt
    ) {}

    /**
     * @param unread cantidad de notificaciones sin leer del representante
     */
    public record UnreadCountResponse(long unread) {}
}
