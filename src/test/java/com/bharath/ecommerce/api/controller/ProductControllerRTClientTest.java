package com.bharath.ecommerce.api.controller;

import com.bharath.ecommerce.api.dto.CreateProductRequest;
import com.bharath.ecommerce.api.dto.ProductResponse;
import com.bharath.ecommerce.api.exception.DuplicateResourceException;
import com.bharath.ecommerce.api.exception.ResourceNotFoundException;
import com.bharath.ecommerce.api.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;

@WebMvcTest(ProductController.class)
@AutoConfigureRestTestClient
class ProductControllerRTClientTest {

    private static final String PRODUCTS_URI = "/api/v1/products";

    @MockitoBean
    private ProductService productService;

    @Autowired
    private RestTestClient restTestClient;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void should_return_200_with_all_products_when_products_are_listed() {
        // Given
        given(productService.getAll()).willReturn(List.of(
                productResponse(1L, "Standing Desk", "DESK-001"),
                productResponse(2L, "Ergonomic Chair", "CHAIR-001")));

        // When
        var response = restTestClient.get()
                .uri(PRODUCTS_URI)
                .accept(MediaType.APPLICATION_JSON)
                .exchange();

        // Then
        response.expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$").isArray()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].id").isEqualTo(1)
                .jsonPath("$[0].name").isEqualTo("Standing Desk")
                .jsonPath("$[0].sku").isEqualTo("DESK-001")
                .jsonPath("$[1].id").isEqualTo(2)
                .jsonPath("$[1].sku").isEqualTo("CHAIR-001");
        then(productService).should().getAll();
    }

    @Test
    void should_return_200_with_empty_list_when_no_product_exists() {
        // Given
        given(productService.getAll()).willReturn(List.of());

        // When
        var response = restTestClient.get()
                .uri(PRODUCTS_URI)
                .accept(MediaType.APPLICATION_JSON)
                .exchange();

        // Then
        response.expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$").isArray()
                .jsonPath("$.length()").isEqualTo(0);
        then(productService).should().getAll();
    }

    @Test
    void should_return_201_with_location_and_body_when_product_is_created() {
        // Given
        given(productService.create(any(CreateProductRequest.class)))
                .willReturn(productResponse(9L, "Standing Desk", "DESK-001"));

        // When
        var response = restTestClient.post()
                .uri(PRODUCTS_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .body(jsonMapper.writeValueAsString(validRequest()))
                .exchange();

        // Then
        response.expectStatus().isCreated()
                .expectHeader().location("http://localhost" + PRODUCTS_URI + "/9")
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.id").isEqualTo(9)
                .jsonPath("$.name").isEqualTo("Standing Desk")
                .jsonPath("$.sku").isEqualTo("DESK-001")
                .jsonPath("$.price").isEqualTo(249.99)
                .jsonPath("$.categoryId").isEqualTo(3);
        then(productService).should().create(any(CreateProductRequest.class));
    }

    @Test
    void should_return_400_with_all_field_errors_when_request_is_invalid() {
        // Given
        var request = CreateProductRequest.builder().name("  ").sku("")
                .price(new BigDecimal("0.00")).stockQuantity(-1).categoryId(0L).build();

        // When
        var response = restTestClient.post()
                .uri(PRODUCTS_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .body(jsonMapper.writeValueAsString(request))
                .exchange();

        // Then
        response.expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.error").isEqualTo("Bad Request")
                .jsonPath("$.message").isEqualTo("Request validation failed")
                .jsonPath("$.path").isEqualTo(PRODUCTS_URI)
                .jsonPath("$.validationErrors.name").exists()
                .jsonPath("$.validationErrors.sku").exists()
                .jsonPath("$.validationErrors.price").exists()
                .jsonPath("$.validationErrors.stockQuantity").exists()
                .jsonPath("$.validationErrors.categoryId").exists();
        then(productService).should(never()).create(any());
    }

    @Test
    void should_return_400_when_mandatory_fields_are_missing() {
        // Given
        var request = CreateProductRequest.builder().name("Standing Desk").sku("DESK-001").build();

        // When
        var response = restTestClient.post()
                .uri(PRODUCTS_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .body(jsonMapper.writeValueAsString(request))
                .exchange();

        // Then
        response.expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.message").isEqualTo("Request validation failed")
                .jsonPath("$.validationErrors.price").exists()
                .jsonPath("$.validationErrors.stockQuantity").exists()
                .jsonPath("$.validationErrors.categoryId").exists()
                .jsonPath("$.validationErrors.name").doesNotExist();
        then(productService).should(never()).create(any());
    }

    @Test
    void should_return_409_when_product_sku_already_exists() {
        // Given
        given(productService.create(any(CreateProductRequest.class)))
                .willThrow(new DuplicateResourceException("Product SKU already exists: DESK-001"));

        // When
        var response = restTestClient.post()
                .uri(PRODUCTS_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .body(jsonMapper.writeValueAsString(validRequest()))
                .exchange();

        // Then
        response.expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody()
                .jsonPath("$.status").isEqualTo(409)
                .jsonPath("$.error").isEqualTo("Conflict")
                .jsonPath("$.message").isEqualTo("Product SKU already exists: DESK-001")
                .jsonPath("$.path").isEqualTo(PRODUCTS_URI)
                .jsonPath("$.validationErrors").doesNotExist();
        then(productService).should().create(any(CreateProductRequest.class));
    }

    @Test
    void should_return_404_when_category_does_not_exist() {
        // Given
        given(productService.create(any(CreateProductRequest.class)))
                .willThrow(new ResourceNotFoundException("Category not found with id 3"));

        // When
        var response = restTestClient.post()
                .uri(PRODUCTS_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .body(jsonMapper.writeValueAsString(validRequest()))
                .exchange();

        // Then
        response.expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.error").isEqualTo("Not Found")
                .jsonPath("$.message").isEqualTo("Category not found with id 3")
                .jsonPath("$.path").isEqualTo(PRODUCTS_URI)
                .jsonPath("$.validationErrors").doesNotExist();
        then(productService).should().create(any(CreateProductRequest.class));
    }

    private CreateProductRequest validRequest() {
        return CreateProductRequest.builder().name("Standing Desk").description("Height adjustable")
                .price(new BigDecimal("249.99")).sku("DESK-001").stockQuantity(12).active(true)
                .categoryId(3L).build();
    }

    private ProductResponse productResponse(Long id, String name, String sku) {
        return ProductResponse.builder().id(id).name(name).description("Height adjustable")
                .price(new BigDecimal("249.99")).sku(sku).stockQuantity(12).active(true)
                .categoryId(3L).categoryName("Home Office").build();
    }
}
