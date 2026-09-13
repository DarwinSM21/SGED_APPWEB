package org.uteq.backend.deportivo.match.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class RosterDtos {
    public record CalledUpPlayer(
            Long studentId,
            String fullName,
            String position,
            Long positionId,
            boolean starter,
            BigDecimal average,
            long attendanceRecords,
            long trainingSessions
    ) {}

    public record UnavailablePlayer(
            Long studentId,
            String fullName,
            String reason
    ) {}

    public record PerformanceWindow(
            int weeks,
            LocalDate from,
            LocalDate to,
            long trainingSessions
    ) {}

    public record LineupResponse(
            Long matchId,
            Long categoryId,
            String category,
            LocalDate date,
            boolean saved,
            Short rating,
            String note,
            PerformanceWindow window,
            List<CalledUpPlayer> starters,
            List<CalledUpPlayer> substitutes,
            List<CalledUpPlayer> available,
            List<UnavailablePlayer> notCallable,
            int starterSlots,
            boolean closed
    ) {}

    public record LineupFeedbackResponse(
            String comment,
            boolean available,
            String reason
    ) {}
}
