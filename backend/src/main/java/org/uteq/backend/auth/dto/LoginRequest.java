package org.uteq.backend.auth.dto;

public record LoginRequest(
        String username,
        String password
) {}