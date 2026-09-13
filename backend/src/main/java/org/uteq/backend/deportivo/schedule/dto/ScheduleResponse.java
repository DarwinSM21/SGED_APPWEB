package org.uteq.backend.deportivo.schedule.dto;

import java.time.LocalTime;

public record ScheduleResponse(
        Long scheduleId,
        Long categoryId,
        String category,
        Integer dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        String field,
        String description,
        Boolean active,
        String conflictsWith
) {}
