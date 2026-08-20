package com.ecommerce.cart.dto;

import com.ecommerce.cart.model.Cart;
import com.ecommerce.cart.model.CartItem;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CartResponse(
        Long userId,
        List<CartItem> items,
        BigDecimal totalAmount,
        int totalItems,
        Instant updatedAt
) {
    public static CartResponse fromModel(Cart cart) {
        int totalItems = cart.getItems() == null ? 0 :
                cart.getItems().stream().mapToInt(CartItem::getQuantity).sum();

        return new CartResponse(
                cart.getUserId(),
                cart.getItems(),
                cart.getTotalAmount(),
                totalItems,
                cart.getUpdatedAt()
        );
    }
}
