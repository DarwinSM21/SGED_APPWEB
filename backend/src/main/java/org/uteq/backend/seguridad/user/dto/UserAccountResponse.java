package org.uteq.backend.seguridad.user.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record UserAccountResponse(
        Long idUsuario,
        Long idPersona,
        String nombrePersona,
        String apellidoPersona,
        String correoPersona,
        Long idEstadoGeneral,
        String estadoGeneralNombre,
        String username,
        List<String> roles,
        OffsetDateTime ultimoAcceso,
        Boolean activo,
        OffsetDateTime createdAt
) {}
