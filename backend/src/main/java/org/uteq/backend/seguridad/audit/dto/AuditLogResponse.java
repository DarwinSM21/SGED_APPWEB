package org.uteq.backend.seguridad.audit.dto;

import java.time.OffsetDateTime;

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
