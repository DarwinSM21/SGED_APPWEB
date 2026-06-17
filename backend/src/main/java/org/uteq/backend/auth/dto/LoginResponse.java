package org.uteq.backend.auth.dto;
import lombok.Builder;

@Builder
public record LoginResponse(
        String token,
        String username,
        String nombre,
        String rol
) {
}