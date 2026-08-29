package com.bharath.ecommerce.api.service;

import com.bharath.ecommerce.api.dto.CreateProductRequest;
import com.bharath.ecommerce.api.entity.Category;
import com.bharath.ecommerce.api.entity.Product;
import com.bharath.ecommerce.api.exception.DuplicateResourceException;
import com.bharath.ecommerce.api.exception.ResourceNotFoundException;
import com.bharath.ecommerce.api.repository.CategoryRepository;
import com.bharath.ecommerce.api.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;

@SpringJUnitConfig(ProductService.class)
class ProductServiceTest {
    @MockitoBean
    private ProductRepository productRepository;
    @MockitoBean
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductService service;

    @Test
    void should_create_product_when_category_exists() {
        // Given
        Category category = Category.builder().id(7L).name("Audio").build();
        given(categoryRepository.findById(7L)).willReturn(Optional.of(category));
        given(productRepository.save(any(Product.class))).willAnswer(invocation -> invocation.getArgument(0));
        var createProductRequest = CreateProductRequest.builder().name(" Headphones ").sku(" hp-100 ")
                .price(new BigDecimal("79.99")).stockQuantity(12).categoryId(7L).build();

        // When
        var response = service.create(createProductRequest);

        // Then
        assertThat(response.getSku()).isEqualTo("HP-100");
        assertThat(response.getCategoryId()).isEqualTo(7L);
        assertThat(response.getActive()).isTrue();
    }

    @Test
    void should_throw_duplicate_resource_when_sku_exists() {
        // Given
        given(productRepository.existsBySkuIgnoreCase("HP-100")).willReturn(true);
        var createProductRequest = CreateProductRequest.builder().name("Headphones")
                .sku(" hp-100 ").price(BigDecimal.ONE).stockQuantity(1).categoryId(7L).build();

        // When
        var thrown = catchThrowable(() -> service.create(createProductRequest));

        // Then
        assertThat(thrown).isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Product SKU already exists: HP-100");
        then(categoryRepository).should(never()).findById(any());
        then(productRepository).should(never()).save(any());
    }

    @Test
    void should_throw_resource_not_found_when_category_missing() {
        // Given
        given(categoryRepository.findById(99L)).willReturn(Optional.empty());
        var createProductRequest = CreateProductRequest.builder().name("Headphones").sku("HP-100")
                .price(BigDecimal.ONE).stockQuantity(1).categoryId(99L).build();

        // When
        var thrown = catchThrowable(() -> service.create(createProductRequest));

        // Then
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class);
        then(productRepository).should(never()).save(any());
    }
}
