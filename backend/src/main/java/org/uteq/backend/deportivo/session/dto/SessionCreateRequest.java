package org.uteq.backend.deportivo.session.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record SessionCreateRequest(
        @NotNull Long categoryId,
        @NotNull LocalDate date,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        String field
) {}
