package com.ecommerce.order.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateOrderRequest(
        @NotBlank(message = "Shipping address is required")
        String shippingAddress,

        @Email(message = "Invalid customer email")
        String customerEmail
) {}
