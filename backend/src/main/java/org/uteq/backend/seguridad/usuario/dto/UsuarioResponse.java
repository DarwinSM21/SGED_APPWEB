package org.uteq.backend.seguridad.usuario.dto;

import java.time.OffsetDateTime;

public record UsuarioResponse(
        Long idUsuario,
        Long idPersona,
        String nombrePersona,
        String apellidoPersona,
        String correoPersona,
        Long idEstadoGeneral,
        String estadoGeneralNombre,
        String username,
        OffsetDateTime ultimoAcceso,
        Boolean activo,
        OffsetDateTime createdAt
) {}