package com.ecommerce.catalog.service;

import com.ecommerce.catalog.domain.Product;
import com.ecommerce.catalog.dto.ProductRequest;
import com.ecommerce.catalog.dto.ProductResponse;
import com.ecommerce.catalog.repository.ProductRepository;
import com.ecommerce.common.exception.BadRequestException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductResponse createProduct(ProductRequest request) {
        Product product = new Product(
                request.name().trim(),
                request.description(),
                request.price(),
                request.stockQuantity(),
                request.category().trim(),
                request.imageUrl()
        );
        Product saved = productRepository.save(product);
        return ProductResponse.fromEntity(saved);
    }

    public ProductResponse updateProduct(String id, ProductRequest request) {
        Product product = findProductEntityById(id);

        product.setName(request.name().trim());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStockQuantity(request.stockQuantity());
        product.setCategory(request.category().trim());
        product.setImageUrl(request.imageUrl());
        product.setUpdatedAt(Instant.now());

        Product updated = productRepository.save(product);
        return ProductResponse.fromEntity(updated);
    }

    public ProductResponse getProductById(String id) {
        return ProductResponse.fromEntity(findProductEntityById(id));
    }

    public List<ProductResponse> getAllProducts() {
        return productRepository.findByActiveTrue().stream()
                .map(ProductResponse::fromEntity)
                .toList();
    }

    public List<ProductResponse> getProductsByCategory(String category) {
        return productRepository.findByCategoryIgnoreCaseAndActiveTrue(category).stream()
                .map(ProductResponse::fromEntity)
                .toList();
    }

    public List<ProductResponse> searchProducts(String name) {
        return productRepository.findByNameContainingIgnoreCaseAndActiveTrue(name).stream()
                .map(ProductResponse::fromEntity)
                .toList();
    }

    public void deleteProduct(String id) {
        Product product = findProductEntityById(id);
        product.setActive(false);
        product.setUpdatedAt(Instant.now());
        productRepository.save(product);
    }

    public ProductResponse reduceStock(String id, Integer quantity) {
        Product product = findProductEntityById(id);

        if (product.getStockQuantity() < quantity) {
            throw new BadRequestException("Insufficient stock for product '" + product.getName() +
                    "'. Available: " + product.getStockQuantity() + ", Requested: " + quantity);
        }

        product.setStockQuantity(product.getStockQuantity() - quantity);
        product.setUpdatedAt(Instant.now());
        Product updated = productRepository.save(product);
        return ProductResponse.fromEntity(updated);
    }

    private Product findProductEntityById(String id) {
        return productRepository.findById(id)
                .filter(Product::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }
}
