package org.uteq.backend.deportivo.coach.dto;

import java.time.OffsetDateTime;

public record CoachResponse(
        Long coachId,
        Long personId,
        String name,
        String lastName,
        String nationalId,
        String email,
        String phone,
        Long userId,
        String username,
        Long specialtyId,
        String specialtyName,
        Short yearsOfExperience,
        String certification,
        Boolean active,
        OffsetDateTime createdAt
) {}