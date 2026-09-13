package org.uteq.backend.academico.guardian.dto;

import org.uteq.backend.academico.guardian.entity.Notification.Type;

import java.time.Instant;

public final class NotificationDtos {

    private NotificationDtos() {}

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
     * @param noLeidas cantidad de notificaciones sin leer del representante
     */
    public record UnreadCountResponse(long unread) {}
}
