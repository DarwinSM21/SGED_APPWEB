package org.uteq.backend.deportivo.sesion.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record SesionCrearRequest(
        @NotNull Long idCategoria,
        @NotNull LocalDate fecha,
        @NotNull LocalTime horaInicio,
        @NotNull LocalTime horaFin,
        String campo
) {}
