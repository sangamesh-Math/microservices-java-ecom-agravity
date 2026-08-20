package com.ecommerce.notification.consumer;

import com.ecommerce.common.dto.OrderItemDto;
import com.ecommerce.common.event.OrderCreatedEvent;
import com.ecommerce.notification.model.NotificationRecord;
import com.ecommerce.notification.service.EmailNotificationService;
import com.ecommerce.notification.service.PromotionalOfferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderNotificationConsumerTest {

    @Mock
    private EmailNotificationService emailNotificationService;

    @Mock
    private PromotionalOfferService promotionalOfferService;

    private OrderNotificationConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new OrderNotificationConsumer(emailNotificationService, promotionalOfferService);
    }

    @Test
    void shouldConsumeOrderCreatedEventAndStoreNotification() {
        OrderItemDto item = new OrderItemDto("prod-1", "Smartphone", new BigDecimal("799.99"), 1, new BigDecimal("799.99"));
        OrderCreatedEvent event = new OrderCreatedEvent(
                "evt-123",
                999L,
                42L,
                "buyer@domain.com",
                new BigDecimal("799.99"),
                "456 Market St",
                List.of(item),
                Instant.now()
        );

        consumer.consumeOrderCreatedEvent(event);

        List<NotificationRecord> recents = consumer.getRecentNotifications();
        assertFalse(recents.isEmpty());
        assertEquals(999L, recents.get(0).orderId());
        assertEquals("buyer@domain.com", recents.get(0).recipientEmail());

        Optional<NotificationRecord> found = consumer.getNotificationByOrderId(999L);
        assertTrue(found.isPresent());
        assertEquals("DELIVERED_MAILPIT_SMTP", found.get().status());

        verify(emailNotificationService, times(1)).sendOrderConfirmationEmail(any(OrderCreatedEvent.class));
        verify(promotionalOfferService, times(1)).registerCustomerEmail("buyer@domain.com");
    }
}
