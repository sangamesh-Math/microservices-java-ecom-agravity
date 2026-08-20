package com.ecommerce.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RevokeTokenRequest(
        @NotBlank(message = "Refresh token is required")
        String refreshToken
) {}
