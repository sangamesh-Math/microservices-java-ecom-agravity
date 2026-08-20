package com.ecommerce.order.client.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CartResponse(
        Long userId,
        List<CartItemDto> items,
        BigDecimal totalAmount,
        int totalItems,
        Instant updatedAt
) {}
