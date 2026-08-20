package com.ecommerce.notification.service;

import com.ecommerce.common.dto.OrderItemDto;
import com.ecommerce.common.event.OrderCreatedEvent;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final JavaMailSender mailSender;

    @Value("${mail.from.address:orders@ecommerce.local}")
    private String fromAddress;

    @Value("${mail.from.name:E-Commerce Platform}")
    private String fromName;

    public EmailNotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOrderConfirmationEmail(OrderCreatedEvent event) {
        String recipient = event.customerEmail();
        String subject = "Order Confirmation #" + event.orderId() + " - E-Commerce Platform";

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress, fromName);
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(buildOrderHtmlTemplate(event), true);

            mailSender.send(message);
            log.info("Successfully dispatched HTML order confirmation email to: {} for Order #{}", recipient, event.orderId());

        } catch (Exception e) {
            log.error("Failed to send HTML order confirmation email to: {}", recipient, e);
        }
    }

    public void sendPromotionalEmail(String recipient, String promoCode, int discountPercent) {
        String subject = "Special Offer: " + discountPercent + "% OFF Everything This Week!";

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress, fromName);
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(buildPromotionalHtmlTemplate(promoCode, discountPercent), true);

            mailSender.send(message);
            log.info("Successfully dispatched promotional email to: {} with code: {}", recipient, promoCode);

        } catch (Exception e) {
            log.error("Failed to send promotional email to: {}", recipient, e);
        }
    }

    private String buildOrderHtmlTemplate(OrderCreatedEvent event) {
        StringBuilder itemsHtml = new StringBuilder();
        if (event.items() != null) {
            for (OrderItemDto item : event.items()) {
                itemsHtml.append(String.format("""
                    <tr>
                        <td style="padding: 12px; border-bottom: 1px solid #e2e8f0; font-weight: 500; color: #1e293b;">%s</td>
                        <td style="padding: 12px; border-bottom: 1px solid #e2e8f0; text-align: center; color: #64748b;">%d</td>
                        <td style="padding: 12px; border-bottom: 1px solid #e2e8f0; text-align: right; color: #64748b;">$%s</td>
                        <td style="padding: 12px; border-bottom: 1px solid #e2e8f0; text-align: right; font-weight: 600; color: #0f172a;">$%s</td>
                    </tr>
                """,
                        item.productName(),
                        item.quantity(),
                        item.unitPrice(),
                        item.subTotal() != null ? item.subTotal() : item.unitPrice().multiply(java.math.BigDecimal.valueOf(item.quantity()))
                ));
            }
        }

        String orderTime = event.timestamp() != null ? DATE_FORMATTER.format(event.timestamp()) : "Just now";

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f8fafc; margin: 0; padding: 20px; }
                    .container { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); }
                    .header { background: linear-gradient(135deg, #4f46e5 0%%, #7c3aed 100%%); color: #ffffff; padding: 32px 24px; text-align: center; }
                    .content { padding: 32px 24px; }
                    .badge { display: inline-block; padding: 4px 12px; background: #dcfce7; color: #15803d; font-weight: 600; font-size: 12px; border-radius: 9999px; }
                    .table { width: 100%%; border-collapse: collapse; margin-top: 20px; }
                    .total-box { margin-top: 24px; padding: 16px; background: #f1f5f9; border-radius: 8px; text-align: right; }
                    .footer { text-align: center; padding: 24px; color: #94a3b8; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1 style="margin: 0; font-size: 24px; letter-spacing: -0.5px;">E-Commerce Platform</h1>
                        <p style="margin: 8px 0 0 0; opacity: 0.9; font-size: 14px;">Order Confirmation & Receipt</p>
                    </div>
                    <div class="content">
                        <div style="display: flex; justify-content: space-between; align-items: center;">
                            <div>
                                <h2 style="margin: 0; font-size: 18px; color: #0f172a;">Order #%d</h2>
                                <p style="margin: 4px 0 0 0; font-size: 13px; color: #64748b;">Placed on %s</p>
                            </div>
                            <span class="badge">CONFIRMED</span>
                        </div>
                        
                        <div style="margin-top: 20px; padding: 12px; background: #f8fafc; border-left: 4px solid #4f46e5; border-radius: 4px;">
                            <p style="margin: 0; font-size: 13px; color: #475569;"><strong>Shipping Address:</strong> %s</p>
                            <p style="margin: 4px 0 0 0; font-size: 13px; color: #475569;"><strong>Customer Email:</strong> %s</p>
                        </div>

                        <table class="table">
                            <thead>
                                <tr style="background: #f8fafc; font-size: 12px; text-transform: uppercase; color: #64748b;">
                                    <th style="padding: 10px 12px; text-align: left;">Item</th>
                                    <th style="padding: 10px 12px; text-align: center;">Qty</th>
                                    <th style="padding: 10px 12px; text-align: right;">Unit Price</th>
                                    <th style="padding: 10px 12px; text-align: right;">Subtotal</th>
                                </tr>
                            </thead>
                            <tbody>
                                %s
                            </tbody>
                        </table>

                        <div class="total-box">
                            <span style="font-size: 14px; color: #64748b;">Total Amount Paid:</span>
                            <span style="font-size: 22px; font-weight: 700; color: #4f46e5; margin-left: 8px;">$%s</span>
                        </div>
                    </div>
                    <div class="footer">
                        <p style="margin: 0;">Thank you for shopping with us! If you have questions, contact support@ecommerce.local</p>
                        <p style="margin: 4px 0 0 0;">© 2026 E-Commerce Platform Inc. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
        """,
                event.orderId(),
                orderTime,
                event.shippingAddress(),
                event.customerEmail(),
                itemsHtml.toString(),
                event.totalAmount()
        );
    }

    private String buildPromotionalHtmlTemplate(String promoCode, int discountPercent) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background-color: #f8fafc; margin: 0; padding: 20px; }
                    .container { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }
                    .header { background: linear-gradient(135deg, #f59e0b 0%%, #ef4444 100%%); color: #ffffff; padding: 36px 24px; text-align: center; }
                    .content { padding: 32px 24px; text-align: center; }
                    .code-box { display: inline-block; padding: 12px 24px; background: #fef3c7; border: 2px dashed #f59e0b; border-radius: 8px; font-size: 24px; font-weight: 700; color: #b45309; letter-spacing: 2px; margin: 20px 0; }
                    .btn { display: inline-block; padding: 14px 28px; background: #4f46e5; color: #ffffff; text-decoration: none; font-weight: 600; border-radius: 8px; margin-top: 16px; }
                    .footer { text-align: center; padding: 20px; color: #94a3b8; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1 style="margin: 0; font-size: 28px;">WEEKLY SPECIAL OFFER!</h1>
                        <p style="margin: 8px 0 0 0; font-size: 16px; opacity: 0.95;">Save %d%% On Your Next Order</p>
                    </div>
                    <div class="content">
                        <p style="color: #334155; font-size: 16px; line-height: 1.6;">
                            Explore our latest high-performance electronics, gadgets, and accessories. Use this exclusive voucher at checkout:
                        </p>
                        <div class="code-box">%s</div>
                        <p style="color: #64748b; font-size: 14px;">Valid this week only. Don't miss out!</p>
                        <div>
                            <a href="http://localhost:8080/api/products" class="btn" style="color: #ffffff;">Shop Deals Now &rarr;</a>
                        </div>
                    </div>
                    <div class="footer">
                        <p style="margin: 0;">You received this email because you are a registered customer at E-Commerce Platform.</p>
                    </div>
                </div>
            </body>
            </html>
        """,
                discountPercent,
                promoCode
        );
    }
}
