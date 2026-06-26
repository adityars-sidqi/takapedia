package com.takapedia.auth.dto;

import java.time.Instant;
import java.util.UUID;

public record RegisterResponse(
        UUID id,
        String email,
        String role,
        Instant createdAt
) {
}
