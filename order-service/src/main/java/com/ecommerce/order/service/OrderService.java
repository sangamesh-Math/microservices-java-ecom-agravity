package com.ecommerce.order.service;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.common.dto.OrderItemDto;
import com.ecommerce.common.event.OrderCreatedEvent;
import com.ecommerce.common.exception.BadRequestException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.order.client.CartFeignClient;
import com.ecommerce.order.client.CatalogFeignClient;
import com.ecommerce.order.client.dto.CartItemDto;
import com.ecommerce.order.client.dto.CartResponse;
import com.ecommerce.order.client.dto.ReduceStockRequest;
import com.ecommerce.order.domain.Order;
import com.ecommerce.order.domain.OrderItem;
import com.ecommerce.order.domain.OrderStatus;
import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.DirectOrderRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.event.OrderEventProducer;
import com.ecommerce.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final CatalogFeignClient catalogClient;
    private final CartFeignClient cartClient;
    private final OrderEventProducer orderEventProducer;

    public OrderService(OrderRepository orderRepository,
                        CatalogFeignClient catalogClient,
                        CartFeignClient cartClient,
                        OrderEventProducer orderEventProducer) {
        this.orderRepository = orderRepository;
        this.catalogClient = catalogClient;
        this.cartClient = cartClient;
        this.orderEventProducer = orderEventProducer;
    }

    @Transactional
    public OrderResponse checkoutFromCart(Long userId, CreateOrderRequest request) {
        log.info("Processing checkout for user: {}", userId);

        ApiResponse<CartResponse> cartApiResponse;
        try {
            cartApiResponse = cartClient.getCartByUserId(userId);
        } catch (Exception e) {
            log.error("Failed to retrieve cart for user: {}", userId, e);
            throw new BadRequestException("Could not retrieve shopping cart: " + e.getMessage());
        }

        if (cartApiResponse == null || cartApiResponse.data() == null ||
                cartApiResponse.data().items() == null || cartApiResponse.data().items().isEmpty()) {
            throw new BadRequestException("Shopping cart is empty. Add items before checking out.");
        }

        List<CartItemDto> cartItems = cartApiResponse.data().items();
        String customerEmail = (request.customerEmail() != null && !request.customerEmail().isBlank())
                ? request.customerEmail()
                : "user" + userId + "@ecommerce.local";

        Order order = new Order(userId, customerEmail, request.shippingAddress());

        // Reserve stock and create order items
        for (CartItemDto cartItem : cartItems) {
            try {
                catalogClient.reduceStock(cartItem.productId(), new ReduceStockRequest(cartItem.quantity()));
            } catch (Exception ex) {
                log.error("Failed to reserve stock for product: {}", cartItem.productId(), ex);
                throw new BadRequestException("Failed to reserve stock for product '" + cartItem.productName() + "': " + ex.getMessage());
            }

            OrderItem item = new OrderItem(
                    cartItem.productId(),
                    cartItem.productName(),
                    cartItem.unitPrice(),
                    cartItem.quantity()
            );
            order.addItem(item);
        }

        order.recalculateTotal();
        order.setStatus(OrderStatus.CONFIRMED);
        Order savedOrder = orderRepository.save(order);

        // Asynchronously clear cart after successful order creation
        try {
            cartClient.clearCartByUserId(userId);
        } catch (Exception e) {
            log.warn("Failed to clear cart for user: {} after order creation", userId, e);
        }

        // Publish OrderCreatedEvent to Kafka
        publishKafkaEvent(savedOrder);

        return OrderResponse.fromEntity(savedOrder);
    }

    @Transactional
    public OrderResponse createDirectOrder(Long userId, DirectOrderRequest request) {
        log.info("Processing direct order for user: {}", userId);

        String customerEmail = (request.customerEmail() != null && !request.customerEmail().isBlank())
                ? request.customerEmail()
                : "user" + userId + "@ecommerce.local";

        Order order = new Order(userId, customerEmail, request.shippingAddress());

        for (OrderItemDto itemDto : request.items()) {
            try {
                catalogClient.reduceStock(itemDto.productId(), new ReduceStockRequest(itemDto.quantity()));
            } catch (Exception ex) {
                log.error("Failed to reserve stock for product: {}", itemDto.productId(), ex);
                throw new BadRequestException("Failed to reserve stock for product '" + itemDto.productName() + "': " + ex.getMessage());
            }

            OrderItem item = new OrderItem(
                    itemDto.productId(),
                    itemDto.productName(),
                    itemDto.unitPrice(),
                    itemDto.quantity()
            );
            order.addItem(item);
        }

        order.recalculateTotal();
        order.setStatus(OrderStatus.CONFIRMED);
        Order savedOrder = orderRepository.save(order);

        // Publish OrderCreatedEvent to Kafka
        publishKafkaEvent(savedOrder);

        return OrderResponse.fromEntity(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        return OrderResponse.fromEntity(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getUserOrders(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(OrderResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(OrderResponse::fromEntity)
                .toList();
    }

    private void publishKafkaEvent(Order order) {
        List<OrderItemDto> itemDtos = order.getItems().stream()
                .map(item -> new OrderItemDto(
                        item.getProductId(),
                        item.getProductName(),
                        item.getUnitPrice(),
                        item.getQuantity(),
                        item.getSubTotal()
                ))
                .toList();

        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID().toString(),
                order.getId(),
                order.getUserId(),
                order.getCustomerEmail(),
                order.getTotalAmount(),
                order.getShippingAddress(),
                itemDtos,
                Instant.now()
        );

        orderEventProducer.publishOrderCreated(event);
    }
}
