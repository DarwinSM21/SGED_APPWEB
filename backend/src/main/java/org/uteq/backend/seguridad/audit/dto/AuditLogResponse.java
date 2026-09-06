package org.uteq.backend.seguridad.audit.dto;

import java.time.OffsetDateTime;

public record AuditLogResponse(
        Long id,
        OffsetDateTime fecha,
        String usuario,
        String rol,
        String accion,
        String entidad,
        Long entidadId,
        String descripcion
) {
}
