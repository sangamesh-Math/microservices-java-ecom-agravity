package com.ecommerce.common.event;

import com.ecommerce.common.dto.OrderItemDto;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderCreatedEvent(
        String eventId,
        Long orderId,
        Long userId,
        String customerEmail,
        BigDecimal totalAmount,
        String shippingAddress,
        List<OrderItemDto> items,
        Instant timestamp
) {}
