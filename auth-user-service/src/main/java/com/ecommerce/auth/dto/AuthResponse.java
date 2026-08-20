package com.ecommerce.auth.dto;

import com.ecommerce.auth.domain.Role;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long userId,
        String email,
        String fullName,
        Role role,
        long expiresIn
) {
    public static AuthResponse of(String accessToken, String refreshToken, Long userId, String email, String fullName, Role role, long expiresIn) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", userId, email, fullName, role, expiresIn);
    }
}
