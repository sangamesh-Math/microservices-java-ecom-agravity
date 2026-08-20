package com.ecommerce.order.client.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartItemDto(
        String productId,
        String productName,
        BigDecimal unitPrice,
        Integer quantity,
        String imageUrl,
        BigDecimal subTotal
) {}
