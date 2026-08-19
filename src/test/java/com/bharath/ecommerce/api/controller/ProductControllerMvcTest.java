package com.bharath.ecommerce.api.controller;

import com.bharath.ecommerce.api.dto.CreateProductRequest;
import com.bharath.ecommerce.api.dto.ProductResponse;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
class ProductControllerMvcTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper jsonMapper;
    @MockitoBean private ProductService productService;

    @Test
    void should_returnProducts_when_productsExist() throws Exception {
        // Arrange
        var product = ProductResponse.builder().id(11L).name("Headphones").price(new BigDecimal("79.99"))
                .sku("HP-100").stockQuantity(12).active(true).categoryId(7L).categoryName("Audio").build();
        when(productService.getAll()).thenReturn(List.of(product));

        // Act
        var result = mockMvc.perform(get("/api/v1/products"));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(11))
                .andExpect(jsonPath("$[0].sku").value("HP-100"));
    }

    @Test
    void should_createProduct_when_requestIsValid() throws Exception {
        // Arrange
        var request = CreateProductRequest.builder().name("Headphones").price(new BigDecimal("79.99"))
                .sku("HP-100").stockQuantity(12).active(true).categoryId(7L).build();
        var response = ProductResponse.builder().id(11L).name("Headphones").price(new BigDecimal("79.99"))
                .sku("HP-100").stockQuantity(12).active(true).categoryId(7L).build();
        when(productService.create(any(CreateProductRequest.class))).thenReturn(response);

        // Act
        var result = mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)));

        // Assert
        result.andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/products/11"))
                .andExpect(jsonPath("$.id").value(11))
                .andExpect(jsonPath("$.sku").value("HP-100"))
                .andDo(print());
    }

    @Test
    void should_rejectRequest_when_priceIsInvalid() throws Exception {
        // Arrange
        var request = CreateProductRequest.builder().name("Headphones").price(BigDecimal.ZERO)
                .sku("HP-100").stockQuantity(12).categoryId(7L).build();

        // Act
        var result = mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)));

        // Assert
        result.andExpect(status().isBadRequest());
    }
}
