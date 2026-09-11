package org.uteq.backend.academico.guardian.dto;

import org.uteq.backend.academico.guardian.entity.Notification.Type;

import java.time.Instant;

public final class NotificationDtos {

    private NotificationDtos() {}

    public record NotificationResponse(
            Long idNotificacion,
            Long idEstudiante,
            String estudiante,
            Type tipo,
            String mensaje,
            boolean leida,
            Instant creadaEn
    ) {}

    /**
     * @param noLeidas cantidad de notificaciones sin leer del representante
     */
    public record UnreadCountResponse(long noLeidas) {}
}
