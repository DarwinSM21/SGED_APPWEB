package org.uteq.backend.deportivo.coach.dto;

import java.time.OffsetDateTime;

/**
 * Vista de la ficha de un entrenador para el cliente.
 *
 * @param coachId            identificador del entrenador
 * @param personId           identificador de la persona vinculada
 * @param name               nombres de la persona
 * @param lastName           apellidos de la persona
 * @param nationalId         cédula de la persona
 * @param email              correo de contacto
 * @param phone              teléfono de contacto
 * @param userId             identificador de la cuenta de usuario
 * @param username           nombre de usuario
 * @param specialtyId        identificador de la especialidad
 * @param specialtyName      nombre de la especialidad
 * @param yearsOfExperience  años de experiencia
 * @param certification      certificación o título
 * @param active             {@code true} si la ficha está activa
 * @param createdAt          fecha de creación
 */
public record CoachResponse(
        Long coachId,
        Long personId,
        String name,
        String lastName,
        String nationalId,
        String email,
        String phone,
        Long userId,
        String username,
        Long specialtyId,
        String specialtyName,
        Short yearsOfExperience,
        String certification,
        Boolean active,
        OffsetDateTime createdAt
) {}