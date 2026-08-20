package com.ecommerce.common.dto;

import java.util.List;

public record UserPrincipalDto(
        Long userId,
        String email,
        String fullName,
        List<String> roles
) {}
