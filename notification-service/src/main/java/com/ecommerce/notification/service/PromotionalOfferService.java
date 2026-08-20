package com.ecommerce.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PromotionalOfferService {

    private static final Logger log = LoggerFactory.getLogger(PromotionalOfferService.class);

    private final EmailNotificationService emailService;

    // Active customer mailing list cache (populated dynamically as users place orders or register)
    private final Set<String> subscribedCustomers = ConcurrentHashMap.newKeySet();

    public PromotionalOfferService(EmailNotificationService emailService) {
        this.emailService = emailService;
        // Default seed subscriber
        this.subscribedCustomers.add("jane.doe@example.com");
    }

    public void registerCustomerEmail(String email) {
        if (email != null && !email.isBlank()) {
            subscribedCustomers.add(email.trim().toLowerCase());
        }
    }

    @Scheduled(cron = "${promotions.schedule.cron:0 0 10 * * MON}")
    public void sendWeeklyPromotionalCampaign() {
        log.info("📢 [PROMOTIONS SERVICE] Executing automated weekly promotional campaign...");
        String promoCode = "WEEKLY" + java.time.LocalDate.now().getDayOfMonth();
        int discount = 20;

        triggerCampaign(promoCode, discount, List.copyOf(subscribedCustomers));
    }

    public int triggerCampaign(String promoCode, int discountPercent, List<String> targetEmails) {
        List<String> recipients = (targetEmails != null && !targetEmails.isEmpty())
                ? targetEmails
                : List.copyOf(subscribedCustomers);

        log.info("📢 Triggering promotional campaign '{}' ({}% OFF) to {} subscribers",
                promoCode, discountPercent, recipients.size());

        for (String recipient : recipients) {
            emailService.sendPromotionalEmail(recipient, promoCode, discountPercent);
        }

        return recipients.size();
    }

    public Set<String> getSubscribedCustomers() {
        return Set.copyOf(subscribedCustomers);
    }
}
