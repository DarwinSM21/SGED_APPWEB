package org.uteq.backend.academico.representante.dto;

import org.uteq.backend.academico.representante.entity.Notificacion.Tipo;

import java.time.Instant;

public final class NotificacionDtos {

    private NotificacionDtos() {}

    public record NotificacionResponse(
            Long idNotificacion,
            Long idEstudiante,
            String estudiante,
            Tipo tipo,
            String mensaje,
            boolean leida,
            Instant creadaEn
    ) {}

    public record ConteoNoLeidasResponse(long noLeidas) {}
}
