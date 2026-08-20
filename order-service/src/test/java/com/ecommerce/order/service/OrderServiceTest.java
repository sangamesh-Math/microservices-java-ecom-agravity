package com.ecommerce.order.service;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.common.event.OrderCreatedEvent;
import com.ecommerce.order.client.CartFeignClient;
import com.ecommerce.order.client.CatalogFeignClient;
import com.ecommerce.order.client.dto.CartItemDto;
import com.ecommerce.order.client.dto.CartResponse;
import com.ecommerce.order.client.dto.ProductResponse;
import com.ecommerce.order.domain.Order;
import com.ecommerce.order.domain.OrderStatus;
import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.event.OrderEventProducer;
import com.ecommerce.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CatalogFeignClient catalogClient;

    @Mock
    private CartFeignClient cartClient;

    @Mock
    private OrderEventProducer orderEventProducer;

    @InjectMocks
    private OrderService orderService;

    @Test
    void shouldCheckoutFromCartSuccessfully() {
        Long userId = 100L;
        CartItemDto cartItem = new CartItemDto("prod-1", "Mechanical Keyboard", new BigDecimal("120.00"), 2, "img.jpg", new BigDecimal("240.00"));
        CartResponse cartResponse = new CartResponse(userId, List.of(cartItem), new BigDecimal("240.00"), 2, Instant.now());

        when(cartClient.getCartByUserId(userId)).thenReturn(ApiResponse.success(cartResponse));
        when(catalogClient.reduceStock(eq("prod-1"), any())).thenReturn(ApiResponse.success(null));

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(5001L);
            return order;
        });

        CreateOrderRequest request = new CreateOrderRequest("123 Tech Blvd, Silicon Valley", "alice@example.com");
        OrderResponse response = orderService.checkoutFromCart(userId, request);

        assertNotNull(response);
        assertEquals(5001L, response.id());
        assertEquals(OrderStatus.CONFIRMED, response.status());
        assertEquals(new BigDecimal("240.00"), response.totalAmount());
        assertEquals(1, response.items().size());

        verify(catalogClient, times(1)).reduceStock(eq("prod-1"), any());
        verify(cartClient, times(1)).clearCartByUserId(userId);
        verify(orderEventProducer, times(1)).publishOrderCreated(any(OrderCreatedEvent.class));
    }
}
