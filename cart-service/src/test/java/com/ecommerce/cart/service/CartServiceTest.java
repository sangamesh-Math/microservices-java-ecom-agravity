package com.ecommerce.cart.service;

import com.ecommerce.cart.dto.AddToCartRequest;
import com.ecommerce.cart.dto.CartResponse;
import com.ecommerce.cart.model.Cart;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private CartService cartService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        cartService = new CartService(redisTemplate, objectMapper);
        ReflectionTestUtils.setField(cartService, "ttlHours", 72L);
    }

    @Test
    void shouldAddItemToCartSuccessfully() {
        when(valueOperations.get("cart:1")).thenReturn(null);

        AddToCartRequest request = new AddToCartRequest(
                "prod-1",
                "Wireless Headphones",
                new BigDecimal("99.99"),
                2,
                "https://example.com/headphones.jpg"
        );

        CartResponse response = cartService.addItemToCart(1L, request);

        assertNotNull(response);
        assertEquals(1L, response.userId());
        assertEquals(1, response.items().size());
        assertEquals(new BigDecimal("199.98"), response.totalAmount());
        assertEquals(2, response.totalItems());
        verify(valueOperations, times(1)).set(eq("cart:1"), any(Cart.class), eq(72L), eq(TimeUnit.HOURS));
    }
}
