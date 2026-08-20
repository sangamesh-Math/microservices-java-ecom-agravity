package com.ecommerce.cart.service;

import com.ecommerce.cart.dto.AddToCartRequest;
import com.ecommerce.cart.dto.CartResponse;
import com.ecommerce.cart.dto.UpdateCartItemRequest;
import com.ecommerce.cart.model.Cart;
import com.ecommerce.cart.model.CartItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class CartService {

    private static final Logger log = LoggerFactory.getLogger(CartService.class);
    private static final String CART_KEY_PREFIX = "cart:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${cart.ttl-hours:72}")
    private long ttlHours;

    public CartService(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public CartResponse getCart(Long userId) {
        Cart cart = getOrCreateCartEntity(userId);
        return CartResponse.fromModel(cart);
    }

    public CartResponse addItemToCart(Long userId, AddToCartRequest request) {
        Cart cart = getOrCreateCartEntity(userId);

        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(request.productId()))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + request.quantity());
            item.setUnitPrice(request.unitPrice());
        } else {
            CartItem newItem = new CartItem(
                    request.productId(),
                    request.productName(),
                    request.unitPrice(),
                    request.quantity(),
                    request.imageUrl()
            );
            cart.getItems().add(newItem);
        }

        cart.recalculateTotal();
        saveCart(cart);
        return CartResponse.fromModel(cart);
    }

    public CartResponse updateItemQuantity(Long userId, UpdateCartItemRequest request) {
        Cart cart = getOrCreateCartEntity(userId);

        if (request.quantity() <= 0) {
            cart.getItems().removeIf(item -> item.getProductId().equals(request.productId()));
        } else {
            cart.getItems().stream()
                    .filter(item -> item.getProductId().equals(request.productId()))
                    .findFirst()
                    .ifPresent(item -> item.setQuantity(request.quantity()));
        }

        cart.recalculateTotal();
        saveCart(cart);
        return CartResponse.fromModel(cart);
    }

    public CartResponse removeItemFromCart(Long userId, String productId) {
        Cart cart = getOrCreateCartEntity(userId);
        cart.getItems().removeIf(item -> item.getProductId().equals(productId));
        cart.recalculateTotal();
        saveCart(cart);
        return CartResponse.fromModel(cart);
    }

    public void clearCart(Long userId) {
        String key = buildKey(userId);
        redisTemplate.delete(key);
        log.info("Cleared cart for user: {}", userId);
    }

    private Cart getOrCreateCartEntity(Long userId) {
        String key = buildKey(userId);
        Object obj = redisTemplate.opsForValue().get(key);

        if (obj != null) {
            try {
                if (obj instanceof Cart cart) {
                    return cart;
                }
                return objectMapper.convertValue(obj, Cart.class);
            } catch (Exception e) {
                log.warn("Failed to deserialize cart for user {}, creating fresh cart", userId, e);
            }
        }

        return new Cart(userId);
    }

    private void saveCart(Cart cart) {
        String key = buildKey(cart.getUserId());
        redisTemplate.opsForValue().set(key, cart, ttlHours, TimeUnit.HOURS);
    }

    private String buildKey(Long userId) {
        return CART_KEY_PREFIX + userId;
    }
}
