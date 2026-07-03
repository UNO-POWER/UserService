package com.unopower.userservice.dto;

public record AuthResponse(
        String id,
        String username,
        String token
) {
}
