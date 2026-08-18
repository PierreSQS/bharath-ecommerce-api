package com.bharath.ecommerce.api.service;

import com.bharath.ecommerce.api.dto.CreateProductRequest;
import com.bharath.ecommerce.api.dto.ProductResponse;
import com.bharath.ecommerce.api.entity.Category;
import com.bharath.ecommerce.api.entity.Product;
import com.bharath.ecommerce.api.exception.DuplicateResourceException;
import com.bharath.ecommerce.api.exception.ResourceNotFoundException;
import com.bharath.ecommerce.api.repository.CategoryRepository;
import com.bharath.ecommerce.api.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        String sku = request.getSku().trim().toUpperCase(Locale.ROOT);
        if (productRepository.existsBySkuIgnoreCase(sku)) {
            throw new DuplicateResourceException("Product SKU already exists: " + sku);
        }
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found with id " + request.getCategoryId()));

        Product product = Product.builder()
                .name(request.getName().trim())
                .description(trimToNull(request.getDescription()))
                .price(request.getPrice())
                .sku(sku)
                .stockQuantity(request.getStockQuantity())
                .active(request.getActive() == null || request.getActive())
                .category(category)
                .build();
        return toResponse(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAll() {
        return productRepository.findAll().stream().map(this::toResponse).toList();
    }

    private ProductResponse toResponse(Product product) {
        return ProductResponse.builder().id(product.getId()).name(product.getName())
                .description(product.getDescription()).price(product.getPrice()).sku(product.getSku())
                .stockQuantity(product.getStockQuantity()).active(product.getActive())
                .categoryId(product.getCategory().getId()).categoryName(product.getCategory().getName()).build();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
