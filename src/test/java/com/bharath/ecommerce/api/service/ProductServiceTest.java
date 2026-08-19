package com.bharath.ecommerce.api.service;

import com.bharath.ecommerce.api.dto.CreateProductRequest;
import com.bharath.ecommerce.api.entity.Category;
import com.bharath.ecommerce.api.entity.Product;
import com.bharath.ecommerce.api.exception.DuplicateResourceException;
import com.bharath.ecommerce.api.exception.ResourceNotFoundException;
import com.bharath.ecommerce.api.repository.CategoryRepository;
import com.bharath.ecommerce.api.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryRepository categoryRepository;

    @Test
    void createsProductForExistingCategory() {
        Category category = Category.builder().id(7L).name("Audio").build();
        when(categoryRepository.findById(7L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ProductService service = new ProductService(productRepository, categoryRepository);

        var response = service.create(CreateProductRequest.builder().name(" Headphones ").sku(" hp-100 ")
                .price(new BigDecimal("79.99")).stockQuantity(12).categoryId(7L).build());

        assertThat(response.getSku()).isEqualTo("HP-100");
        assertThat(response.getCategoryId()).isEqualTo(7L);
        assertThat(response.getActive()).isTrue();
    }

    @Test
    void rejectsDuplicateSku() {
        when(productRepository.existsBySkuIgnoreCase("HP-100")).thenReturn(true);
        ProductService service = new ProductService(productRepository, categoryRepository);

        assertThatThrownBy(() -> service.create(CreateProductRequest.builder().name("Headphones")
                .sku(" hp-100 ").price(BigDecimal.ONE).stockQuantity(1).categoryId(7L).build()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Product SKU already exists: HP-100");
        verify(categoryRepository, never()).findById(any());
        verify(productRepository, never()).save(any());
    }

    @Test
    void rejectsProductWithUnknownCategory() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());
        ProductService service = new ProductService(productRepository, categoryRepository);

        assertThatThrownBy(() -> service.create(CreateProductRequest.builder().name("Headphones").sku("HP-100")
                .price(BigDecimal.ONE).stockQuantity(1).categoryId(99L).build()))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(productRepository, never()).save(any());
    }
}
