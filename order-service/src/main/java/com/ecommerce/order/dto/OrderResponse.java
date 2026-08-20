package com.ecommerce.order.dto;

import com.ecommerce.common.dto.OrderItemDto;
import com.ecommerce.order.domain.Order;
import com.ecommerce.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        Long userId,
        String customerEmail,
        OrderStatus status,
        BigDecimal totalAmount,
        String shippingAddress,
        List<OrderItemDto> items,
        Instant createdAt,
        Instant updatedAt
) {
    public static OrderResponse fromEntity(Order order) {
        List<OrderItemDto> itemDtos = order.getItems().stream()
                .map(item -> new OrderItemDto(
                        item.getProductId(),
                        item.getProductName(),
                        item.getUnitPrice(),
                        item.getQuantity(),
                        item.getSubTotal()
                ))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getCustomerEmail(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getShippingAddress(),
                itemDtos,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
