package org.uteq.backend.academico.guardian.dto;

import java.time.Instant;
import java.util.List;

/**
 * Vista de la ficha de un representante para el cliente.
 *
 * @param guardianId    identificador del representante
 * @param personId      identificador de la persona vinculada
 * @param name          nombres de la persona
 * @param lastName      apellidos de la persona
 * @param nationalId    cédula de la persona
 * @param email         correo de contacto
 * @param userId        identificador de la cuenta de usuario, si tiene
 * @param username      nombre de usuario, si tiene cuenta
 * @param relationship  relación general declarada con sus estudiantes
 * @param contactPhone  teléfono de contacto
 * @param active        {@code true} si la ficha está activa
 * @param createdAt     fecha de creación de la ficha
 * @param wards         estudiantes vinculados a este representante
 */
public record GuardianResponse(
        Long guardianId,
        Long personId,
        String name,
        String lastName,
        String nationalId,
        String email,
        Long userId,
        String username,
        String relationship,
        String contactPhone,
        Boolean active,
        Instant createdAt,
        List<LinkedStudentResponse> wards
) {
    /**
     * Un estudiante vinculado a un representante.
     *
     * @param studentId      identificador del estudiante
     * @param fullName       nombre completo del estudiante
     * @param category       categoría deportiva del estudiante
     * @param relationship   relación de este representante con este estudiante en particular
     * @param primaryContact {@code true} si es el contacto principal del estudiante
     */
    public record LinkedStudentResponse(
            Long studentId,
            String fullName,
            String category,
            String relationship,
            Boolean primaryContact
    ) {}
}
