package com.ecommerce.common.dto;

import java.math.BigDecimal;

public record OrderItemDto(
        String productId,
        String productName,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal subTotal
) {
    public OrderItemDto {
        if (subTotal == null && unitPrice != null && quantity != null) {
            subTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }
}
