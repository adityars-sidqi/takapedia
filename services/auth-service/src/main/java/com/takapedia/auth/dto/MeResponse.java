package com.takapedia.auth.dto;

public record MeResponse(
        String userId,
        String role
) {}