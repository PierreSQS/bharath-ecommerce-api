package com.bharath.ecommerce.api.controller;

import com.bharath.ecommerce.api.dto.CreateProductRequest;
import com.bharath.ecommerce.api.dto.ProductResponse;
import com.bharath.ecommerce.api.exception.DuplicateResourceException;
import com.bharath.ecommerce.api.exception.ResourceNotFoundException;
import com.bharath.ecommerce.api.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
class ProductControllerMvcTest {

    private static final String PRODUCTS_URI = "/api/v1/products";

    @MockitoBean
    private ProductService productService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void should_return_200_with_all_products_when_products_are_listed() throws Exception {
        // Given
        given(productService.getAll()).willReturn(List.of(
                productResponse(1L, "Standing Desk", "DESK-001"),
                productResponse(2L, "Ergonomic Chair", "CHAIR-001")));

        // When
        var result = mockMvc.perform(get(PRODUCTS_URI));

        // Then
        result.andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Standing Desk"))
                .andExpect(jsonPath("$[0].sku").value("DESK-001"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].sku").value("CHAIR-001"));
        then(productService).should().getAll();
    }

    @Test
    void should_return_200_with_empty_list_when_no_product_exists() throws Exception {
        // Given
        given(productService.getAll()).willReturn(List.of());

        // When
        var result = mockMvc.perform(get(PRODUCTS_URI));

        // Then
        result.andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));
        then(productService).should().getAll();
    }

    @Test
    void should_return_201_with_location_and_body_when_product_is_created() throws Exception {
        // Given
        given(productService.create(any(CreateProductRequest.class)))
                .willReturn(productResponse(9L, "Standing Desk", "DESK-001"));

        // When
        var result = mockMvc.perform(post(PRODUCTS_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(validRequest())));

        // Then
        result.andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost" + PRODUCTS_URI + "/9"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.name").value("Standing Desk"))
                .andExpect(jsonPath("$.sku").value("DESK-001"))
                .andExpect(jsonPath("$.price").value(249.99))
                .andExpect(jsonPath("$.categoryId").value(3));
        then(productService).should().create(any(CreateProductRequest.class));
    }

    @Test
    void should_return_400_with_all_field_errors_when_request_is_invalid() throws Exception {
        // Given
        var request = CreateProductRequest.builder().name("  ").sku("")
                .price(new BigDecimal("0.00")).stockQuantity(-1).categoryId(0L).build();

        // When
        var result = mockMvc.perform(post(PRODUCTS_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)));

        // Then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.path").value(PRODUCTS_URI))
                .andExpect(jsonPath("$.validationErrors.name").exists())
                .andExpect(jsonPath("$.validationErrors.sku").exists())
                .andExpect(jsonPath("$.validationErrors.price").exists())
                .andExpect(jsonPath("$.validationErrors.stockQuantity").exists())
                .andExpect(jsonPath("$.validationErrors.categoryId").exists());
        then(productService).should(never()).create(any());
    }

    @Test
    void should_return_400_when_mandatory_fields_are_missing() throws Exception {
        // Given
        var request = CreateProductRequest.builder().name("Standing Desk").sku("DESK-001").build();

        // When
        var result = mockMvc.perform(post(PRODUCTS_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)));

        // Then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.validationErrors.price").exists())
                .andExpect(jsonPath("$.validationErrors.stockQuantity").exists())
                .andExpect(jsonPath("$.validationErrors.categoryId").exists())
                .andExpect(jsonPath("$.validationErrors.name").doesNotExist());
        then(productService).should(never()).create(any());
    }

    @Test
    void should_return_409_when_product_sku_already_exists() throws Exception {
        // Given
        given(productService.create(any(CreateProductRequest.class)))
                .willThrow(new DuplicateResourceException("Product SKU already exists: DESK-001"));

        // When
        var result = mockMvc.perform(post(PRODUCTS_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(validRequest())));

        // Then
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Product SKU already exists: DESK-001"))
                .andExpect(jsonPath("$.path").value(PRODUCTS_URI));
        then(productService).should().create(any(CreateProductRequest.class));
    }

    @Test
    void should_return_404_when_category_does_not_exist() throws Exception {
        // Given
        given(productService.create(any(CreateProductRequest.class)))
                .willThrow(new ResourceNotFoundException("Category not found with id 3"));

        // When
        var result = mockMvc.perform(post(PRODUCTS_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(validRequest())));

        // Then
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Category not found with id 3"))
                .andExpect(jsonPath("$.path").value(PRODUCTS_URI))
                .andExpect(jsonPath("$.validationErrors").doesNotExist());
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
