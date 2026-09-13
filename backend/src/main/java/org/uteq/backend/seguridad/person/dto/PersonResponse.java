package org.uteq.backend.seguridad.person.dto;

import java.time.Instant;
import java.time.LocalDate;

public record PersonResponse(
        Long personId,
        String name,
        String lastName,
        String nationalId,
        String email,
        String phone,
        String photo,
        LocalDate birthDate,
        Boolean active,
        Instant createdAt
) {}
