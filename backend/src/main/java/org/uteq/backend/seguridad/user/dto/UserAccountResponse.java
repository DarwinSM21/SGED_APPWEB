package org.uteq.backend.seguridad.user.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record UserAccountResponse(
        Long userId,
        Long personId,
        String personName,
        String personLastName,
        String personEmail,
        Long generalStatusId,
        String generalStatusName,
        String username,
        List<String> roles,
        OffsetDateTime lastAccess,
        Boolean active,
        OffsetDateTime createdAt
) {}
