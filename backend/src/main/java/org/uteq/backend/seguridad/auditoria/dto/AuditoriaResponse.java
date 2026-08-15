package org.uteq.backend.seguridad.auditoria.dto;

import java.time.OffsetDateTime;

public record AuditoriaResponse(
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
