package org.uteq.backend.seguridad.user.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Vista de una cuenta de usuario para el cliente.
 *
 * @param userId              identificador de la cuenta
 * @param personId            identificador de la persona vinculada
 * @param personName          nombres de la persona
 * @param personLastName      apellidos de la persona
 * @param personEmail         correo de la persona
 * @param generalStatusId     identificador del estado general
 * @param generalStatusName   nombre del estado general
 * @param username            nombre de usuario
 * @param roles               roles asignados a la cuenta
 * @param lastAccess          fecha del último acceso, si tiene
 * @param active              {@code true} si la cuenta está activa
 * @param createdAt           fecha de creación
 */
public record UserAccountResponse(
        Long userId,
        Long personId,
        String personName,
        String personLastName,
        String personEmail,
        Long generalStatusId,
        String generalStatusName,
        String username,
        List<String> roles,
        OffsetDateTime lastAccess,
        Boolean active,
        OffsetDateTime createdAt
) {}
