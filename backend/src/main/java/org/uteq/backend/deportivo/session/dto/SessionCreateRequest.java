package org.uteq.backend.deportivo.session.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record SessionCreateRequest(
        @NotNull Long idCategoria,
        @NotNull LocalDate fecha,
        @NotNull LocalTime horaInicio,
        @NotNull LocalTime horaFin,
        String campo
) {}
