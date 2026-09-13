package org.uteq.backend.deportivo.session.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record SessionTodayResponse(
        Long sessionId,
        String category,
        String coach,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        String field,
        String status,
        boolean hasEvaluation
) {}
