package com.ecommerce.order.client;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.order.client.dto.ProductResponse;
import com.ecommerce.order.client.dto.ReduceStockRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "catalog-service", url = "${services.catalog.url:http://localhost:8082}")
public interface CatalogFeignClient {

    @GetMapping("/api/products/{id}")
    ApiResponse<ProductResponse> getProductById(@PathVariable("id") String id);

    @PostMapping("/api/products/{id}/reduce-stock")
    ApiResponse<ProductResponse> reduceStock(@PathVariable("id") String id, @RequestBody ReduceStockRequest request);
}
