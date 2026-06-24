package org.uteq.backend.auth.dto;

public record RefreshRequest(
        String refreshToken
) {}