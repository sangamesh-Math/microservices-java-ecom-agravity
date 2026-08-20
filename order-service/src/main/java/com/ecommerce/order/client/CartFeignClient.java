package com.ecommerce.order.client;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.order.client.dto.CartResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "cart-service", url = "${services.cart.url:http://localhost:8083}")
public interface CartFeignClient {

    @GetMapping("/api/cart/user/{userId}")
    ApiResponse<CartResponse> getCartByUserId(@PathVariable("userId") Long userId);

    @DeleteMapping("/api/cart/user/{userId}")
    ApiResponse<Void> clearCartByUserId(@PathVariable("userId") Long userId);
}
