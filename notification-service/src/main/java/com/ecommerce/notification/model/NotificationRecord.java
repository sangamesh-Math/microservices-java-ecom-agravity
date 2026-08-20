package com.ecommerce.notification.model;

import com.ecommerce.common.event.OrderCreatedEvent;
import java.time.Instant;

public record NotificationRecord(
        String notificationId,
        Long orderId,
        String recipientEmail,
        String channel,
        String messageSubject,
        String messageBody,
        String status,
        Instant dispatchedAt,
        OrderCreatedEvent eventDetails
) {}
