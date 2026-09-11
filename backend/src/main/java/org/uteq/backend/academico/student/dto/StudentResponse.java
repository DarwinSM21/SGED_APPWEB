package org.uteq.backend.academico.student.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record StudentResponse(
        Long idEstudiante,
        Long idPersona,
        Long idCategoria,
        Long idEstadoGeneral,
        String nombrePersona,
        String apellidoPersona,
        String nombreCategoria,
        String nombreEstadoGeneral,
        String codigoEstudiante,
        LocalDate fechaIngreso,
        BigDecimal peso,
        BigDecimal altura,
        Long idPosicion,
        String nombrePosicion,
        String abreviaturaPosicion,
        Boolean activo,
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
        return new StudentResponse(idEstudiante, idPersona, idCategoria, idEstadoGeneral,
                nombrePersona, apellidoPersona, nombreCategoria, nombreEstadoGeneral,
                codigoEstudiante, fechaIngreso, null, null, idPosicion, nombrePosicion,
                abreviaturaPosicion, activo, createdAt);
    }
}
