package com.ecommerce.catalog.service;

import com.ecommerce.catalog.domain.Product;
import com.ecommerce.catalog.dto.ProductRequest;
import com.ecommerce.catalog.dto.ProductResponse;
import com.ecommerce.catalog.repository.ProductRepository;
import com.ecommerce.common.exception.BadRequestException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        sampleProduct = new Product(
                "Gaming Laptop",
                "High performance laptop",
                new BigDecimal("1499.99"),
                20,
                "Electronics",
                "https://example.com/laptop.jpg"
        );
        sampleProduct.setId("prod-123");
    }

    @Test
    void shouldCreateProductSuccessfully() {
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);

        ProductRequest request = new ProductRequest(
                "Gaming Laptop",
                "High performance laptop",
                new BigDecimal("1499.99"),
                20,
                "Electronics",
                "https://example.com/laptop.jpg"
        );

        ProductResponse response = productService.createProduct(request);

        assertNotNull(response);
        assertEquals("Gaming Laptop", response.name());
        assertEquals(20, response.stockQuantity());
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void shouldReduceStockSuccessfully() {
        when(productRepository.findById("prod-123")).thenReturn(Optional.of(sampleProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

        ProductResponse response = productService.reduceStock("prod-123", 5);

        assertEquals(15, response.stockQuantity());
    }

    @Test
    void shouldThrowWhenStockInsufficient() {
        when(productRepository.findById("prod-123")).thenReturn(Optional.of(sampleProduct));

        assertThrows(BadRequestException.class, () -> productService.reduceStock("prod-123", 50));
    }
}
