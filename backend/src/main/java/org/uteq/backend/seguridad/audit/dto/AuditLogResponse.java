package org.uteq.backend.seguridad.audit.dto;

import java.time.OffsetDateTime;

/**
 * Vista de una entrada de auditoría para el cliente.
 *
 * @param id           identificador del registro
 * @param date         fecha y hora del evento
 * @param user         usuario que realizó la acción
 * @param role         rol del usuario al momento de la acción
 * @param action       acción realizada (crear, editar, eliminar, etc.)
 * @param entity       tipo de entidad afectada
 * @param entityId     identificador de la entidad afectada
 * @param description  descripción legible del evento
 */
public record AuditLogResponse(
        Long id,
        OffsetDateTime date,
        String user,
        String role,
        String action,
        String entity,
        Long entityId,
        String description
) {
}
