package org.uteq.backend.academico.guardian.dto;

import java.time.Instant;
import java.util.List;

public record GuardianResponse(
        Long guardianId,
        Long personId,
        String name,
        String lastName,
        String nationalId,
        String email,
        Long userId,
        String username,
        String relationship,
        String contactPhone,
        Boolean active,
        Instant createdAt,
        List<LinkedStudentResponse> wards
) {
    public record LinkedStudentResponse(
            Long studentId,
            String fullName,
            String category,
            String relationship,
            Boolean primaryContact
    ) {}
}
