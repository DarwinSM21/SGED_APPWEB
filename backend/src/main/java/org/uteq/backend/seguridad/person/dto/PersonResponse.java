package org.uteq.backend.seguridad.person.dto;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Vista de una persona para el cliente.
 *
 * @param personId    identificador de la persona
 * @param name        nombres
 * @param lastName    apellidos
 * @param nationalId  cédula
 * @param email       correo de contacto
 * @param phone       teléfono de contacto
 * @param photo       foto de perfil
 * @param birthDate   fecha de nacimiento
 * @param active      {@code true} si la ficha está activa
 * @param createdAt   fecha de creación
 */
public record PersonResponse(
        Long personId,
        String name,
        String lastName,
        String nationalId,
        String email,
        String phone,
        String photo,
        LocalDate birthDate,
        Boolean active,
        Instant createdAt
) {}
