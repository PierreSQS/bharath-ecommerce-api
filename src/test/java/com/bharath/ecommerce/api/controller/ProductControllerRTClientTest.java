package com.bharath.ecommerce.api.controller;

import com.bharath.ecommerce.api.dto.CreateProductRequest;
import com.bharath.ecommerce.api.dto.ProductResponse;
import com.bharath.ecommerce.api.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
@WebMvcTest(ProductController.class)
@AutoConfigureRestTestClient
class ProductControllerRTClientTest {
    @Autowired private RestTestClient restTestClient;
    @MockitoBean private ProductService productService;

    @Test
    void should_returnProducts_when_productsExist() {
        // Arrange
        var product = ProductResponse.builder().id(11L).name("Headphones").price(new BigDecimal("79.99"))
                .sku("HP-100").stockQuantity(12).active(true).categoryId(7L).categoryName("Audio").build();
        when(productService.getAll()).thenReturn(List.of(product));

        // Act
        var result = restTestClient.get().uri("/api/v1/products").exchange();

        // Assert
        result.expectStatus().isOk().expectBody()
                .jsonPath("$[0].id").isEqualTo(11)
                .jsonPath("$[0].sku").isEqualTo("HP-100");
    }

    @Test
    void should_createProduct_when_requestIsValid() {
        // Arrange
        var request = CreateProductRequest.builder().name("Headphones").price(new BigDecimal("79.99"))
                .sku("HP-100").stockQuantity(12).active(true).categoryId(7L).build();
        var response = ProductResponse.builder().id(11L).name("Headphones").price(new BigDecimal("79.99"))
                .sku("HP-100").stockQuantity(12).active(true).categoryId(7L).build();
        when(productService.create(any(CreateProductRequest.class))).thenReturn(response);

        // Act
        var result = restTestClient.post().uri("/api/v1/products").body(request).exchange();

        // Assert
        result.expectStatus().isCreated()
                .expectHeader().valueEquals("Location", "http://localhost/api/v1/products/11")
                .expectBody().jsonPath("$.id").isEqualTo(11)
                .jsonPath("$.sku").isEqualTo("HP-100");
    }

    @Test
    void should_rejectRequest_when_priceIsInvalid() {
        // Arrange
        var request = CreateProductRequest.builder().name("Headphones").price(BigDecimal.ZERO)
                .sku("HP-100").stockQuantity(12).categoryId(7L).build();

        // Act
        var result = restTestClient.post().uri("/api/v1/products").body(request).exchange();

        // Assert
        result.expectStatus().isBadRequest();
    }
}
