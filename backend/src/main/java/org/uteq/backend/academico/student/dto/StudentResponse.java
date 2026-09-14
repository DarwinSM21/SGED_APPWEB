package org.uteq.backend.academico.student.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Vista de la ficha de un estudiante para el cliente.
 *
 * @param studentId              identificador del estudiante
 * @param personId               identificador de la persona vinculada
 * @param categoryId             identificador de la categoría deportiva
 * @param generalStatusId        identificador del estado general
 * @param personName             nombres de la persona
 * @param personLastName         apellidos de la persona
 * @param categoryName           nombre de la categoría deportiva
 * @param generalStatusName      nombre del estado general
 * @param studentCode            código interno del estudiante
 * @param enrollmentDate         fecha de ingreso
 * @param weight                 peso del estudiante (dato restringido, ver {@link #withoutPhysicalData()})
 * @param height                 altura del estudiante (dato restringido, ver {@link #withoutPhysicalData()})
 * @param positionId             identificador de la posición de juego
 * @param positionName           nombre de la posición
 * @param positionAbbreviation   abreviatura de la posición
 * @param active                 {@code true} si la ficha está activa
 * @param createdAt              fecha de creación de la ficha
 */
public record StudentResponse(
        Long studentId,
        Long personId,
        Long categoryId,
        Long generalStatusId,
        String personName,
        String personLastName,
        String categoryName,
        String generalStatusName,
        String studentCode,
        LocalDate enrollmentDate,
        BigDecimal weight,
        BigDecimal height,
        Long positionId,
        String positionName,
        String positionAbbreviation,
        Boolean active,
        Instant createdAt
) {
    /**
     * RF-11b / hallazgo H-06: {@code peso} y {@code altura} son datos de salud
     * de un menor y su lectura se restringe a {@code ADMINISTRADOR} y
     * {@code ENTRENADOR} (cuerpo técnico). Esta copia los omite para el resto
     * de roles con acceso a la ficha ({@code RECEPCIONISTA}).
     *
     * @return una copia de esta respuesta con {@code peso} y {@code altura} en {@code null}
     */
    public StudentResponse withoutPhysicalData() {
        return new StudentResponse(studentId, personId, categoryId, generalStatusId,
                personName, personLastName, categoryName, generalStatusName,
                studentCode, enrollmentDate, null, null, positionId, positionName,
                positionAbbreviation, active, createdAt);
    }
}
