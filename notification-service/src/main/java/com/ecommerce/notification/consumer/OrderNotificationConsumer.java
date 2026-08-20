package com.ecommerce.notification.consumer;

import com.ecommerce.common.event.OrderCreatedEvent;
import com.ecommerce.notification.model.NotificationRecord;
import com.ecommerce.notification.service.EmailNotificationService;
import com.ecommerce.notification.service.PromotionalOfferService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class OrderNotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderNotificationConsumer.class);

    private final EmailNotificationService emailService;
    private final PromotionalOfferService promotionalOfferService;
    private final List<NotificationRecord> recentNotifications = new CopyOnWriteArrayList<>();
    private final Map<Long, NotificationRecord> notificationsByOrderId = new ConcurrentHashMap<>();

    public OrderNotificationConsumer(EmailNotificationService emailService,
                                   PromotionalOfferService promotionalOfferService) {
        this.emailService = emailService;
        this.promotionalOfferService = promotionalOfferService;
    }

    @KafkaListener(
            topics = "${kafka.topics.order-events:order-events}",
            groupId = "${spring.kafka.consumer.group-id:notification-service-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("================================================================================");
        log.info("🔔 [NOTIFICATION SERVICE] RECEIVED ASYNC KAFKA EVENT: OrderCreatedEvent");
        log.info("--------------------------------------------------------------------------------");
        log.info("Order ID:         #{}", event.orderId());
        log.info("Customer Email:   {}", event.customerEmail());
        log.info("User ID:          {}", event.userId());
        log.info("Total Amount:     ${}", event.totalAmount());
        log.info("Shipping Address: {}", event.shippingAddress());
        log.info("Items Count:      {}", event.items() != null ? event.items().size() : 0);

        if (event.items() != null) {
            event.items().forEach(item ->
                    log.info("  -> Product: {} | Qty: {} | Unit Price: ${} | SubTotal: ${}",
                            item.productName(), item.quantity(), item.unitPrice(), item.subTotal())
            );
        }

        // 1. Dispatch real HTML order confirmation email via SMTP (Mailpit)
        emailService.sendOrderConfirmationEmail(event);

        // 2. Register customer email for weekly promotional campaigns
        promotionalOfferService.registerCustomerEmail(event.customerEmail());

        String subject = "Order Confirmation - Order #" + event.orderId();
        String body = "Dear Customer, thank you for your order #" + event.orderId() +
                ". Total: $" + event.totalAmount() + ". Items will be shipped to: " + event.shippingAddress();

        NotificationRecord notification = new NotificationRecord(
                UUID.randomUUID().toString(),
                event.orderId(),
                event.customerEmail(),
                "EMAIL_SMTP",
                subject,
                body,
                "DELIVERED_MAILPIT_SMTP",
                Instant.now(),
                event
        );

        recentNotifications.add(0, notification);
        notificationsByOrderId.put(event.orderId(), notification);

        if (recentNotifications.size() > 100) {
            recentNotifications.remove(recentNotifications.size() - 1);
        }

        log.info(" HTML invoice email dispatched to SMTP server for: {}", event.customerEmail());
        log.info("================================================================================");
    }

    public List<NotificationRecord> getRecentNotifications() {
        return Collections.unmodifiableList(recentNotifications);
    }

    public Optional<NotificationRecord> getNotificationByOrderId(Long orderId) {
        return Optional.ofNullable(notificationsByOrderId.get(orderId));
    }
}
