package com.ecommerce.order.dto;

import com.ecommerce.common.dto.OrderItemDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record DirectOrderRequest(
        @NotBlank(message = "Shipping address is required")
        String shippingAddress,

        @Email(message = "Invalid customer email")
        String customerEmail,

        @NotEmpty(message = "Items list cannot be empty")
        @Valid
        List<OrderItemDto> items
) {}
