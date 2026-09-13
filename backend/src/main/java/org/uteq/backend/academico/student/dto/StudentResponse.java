package org.uteq.backend.academico.student.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

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
