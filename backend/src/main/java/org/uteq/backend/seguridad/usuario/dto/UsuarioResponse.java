package org.uteq.backend.seguridad.usuario.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record UsuarioResponse(
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