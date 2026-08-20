package com.ecommerce.notification.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.notification.consumer.OrderNotificationConsumer;
import com.ecommerce.notification.model.NotificationRecord;
import com.ecommerce.notification.service.PromotionalOfferService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final OrderNotificationConsumer notificationConsumer;
    private final PromotionalOfferService promotionalOfferService;

    public NotificationController(OrderNotificationConsumer notificationConsumer,
                                  PromotionalOfferService promotionalOfferService) {
        this.notificationConsumer = notificationConsumer;
        this.promotionalOfferService = promotionalOfferService;
    }

    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<NotificationRecord>>> getRecentNotifications() {
        return ResponseEntity.ok(ApiResponse.success(notificationConsumer.getRecentNotifications()));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<NotificationRecord>> getNotificationByOrderId(@PathVariable Long orderId) {
        NotificationRecord record = notificationConsumer.getNotificationByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("No notification found for order ID: " + orderId));
        return ResponseEntity.ok(ApiResponse.success(record));
    }

    @PostMapping("/promotions/trigger")
    public ResponseEntity<ApiResponse<Map<String, Object>>> triggerPromotions(
            @RequestBody(required = false) Map<String, Object> payload
    ) {
        String promoCode = payload != null && payload.containsKey("promoCode") ? (String) payload.get("promoCode") : "PROMO2026";
        int discount = payload != null && payload.containsKey("discountPercent") ? ((Number) payload.get("discountPercent")).intValue() : 20;

        @SuppressWarnings("unchecked")
        List<String> targetEmails = payload != null && payload.containsKey("targetEmails") ? (List<String>) payload.get("targetEmails") : null;

        int sentCount = promotionalOfferService.triggerCampaign(promoCode, discount, targetEmails);

        Map<String, Object> result = Map.of(
                "promoCode", promoCode,
                "discountPercent", discount,
                "emailsSent", sentCount,
                "status", "DISPATCHED"
        );

        return ResponseEntity.ok(ApiResponse.success("Weekly promotional campaign triggered successfully", result));
    }

    @GetMapping("/subscribers")
    public ResponseEntity<ApiResponse<Set<String>>> getSubscribers() {
        return ResponseEntity.ok(ApiResponse.success(promotionalOfferService.getSubscribedCustomers()));
    }
}
