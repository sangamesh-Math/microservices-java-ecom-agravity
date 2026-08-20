package com.ecommerce.order.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.common.exception.BadRequestException;
import com.ecommerce.order.dto.CreateOrderRequest;
import com.ecommerce.order.dto.DirectOrderRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    private Long resolveUserId(Long headerUserId, Long paramUserId) {
        if (headerUserId != null) return headerUserId;
        if (paramUserId != null) return paramUserId;
        throw new BadRequestException("User ID is required via 'X-User-Id' header or 'userId' parameter");
    }

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<OrderResponse>> checkout(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestParam(value = "userId", required = false) Long paramUserId,
            @Valid @RequestBody CreateOrderRequest request
    ) {
        Long userId = resolveUserId(headerUserId, paramUserId);
        OrderResponse orderResponse = orderService.checkoutFromCart(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order placed successfully from cart", orderResponse));
    }

    @PostMapping("/direct")
    public ResponseEntity<ApiResponse<OrderResponse>> createDirectOrder(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestParam(value = "userId", required = false) Long paramUserId,
            @Valid @RequestBody DirectOrderRequest request
    ) {
        Long userId = resolveUserId(headerUserId, paramUserId);
        OrderResponse orderResponse = orderService.createDirectOrder(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Direct order placed successfully", orderResponse));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getOrderById(id)));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrdersByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getUserOrders(userId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrders(
            @RequestHeader(value = "X-User-Id", required = false) Long headerUserId,
            @RequestParam(value = "userId", required = false) Long paramUserId
    ) {
        if (headerUserId != null) {
            return ResponseEntity.ok(ApiResponse.success(orderService.getUserOrders(headerUserId)));
        }
        if (paramUserId != null) {
            return ResponseEntity.ok(ApiResponse.success(orderService.getUserOrders(paramUserId)));
        }
        return ResponseEntity.ok(ApiResponse.success(orderService.getAllOrders()));
    }
}
