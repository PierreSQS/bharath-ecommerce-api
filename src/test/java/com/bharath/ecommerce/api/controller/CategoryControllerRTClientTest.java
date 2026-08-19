package com.bharath.ecommerce.api.controller;

import com.bharath.ecommerce.api.dto.CategoryResponse;
import com.bharath.ecommerce.api.dto.CreateCategoryRequest;
import com.bharath.ecommerce.api.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
@WebMvcTest(CategoryController.class)
@AutoConfigureRestTestClient
class CategoryControllerRTClientTest {
    @Autowired private RestTestClient restTestClient;
    @MockitoBean private CategoryService categoryService;

    @Test
    void should_createCategory_when_requestIsValid() {
        // Arrange
        var request = CreateCategoryRequest.builder().name("Audio").slug("audio").description("Audio gear").build();
        var response = CategoryResponse.builder().id(7L).name("Audio").slug("audio").description("Audio gear").build();
        when(categoryService.create(any(CreateCategoryRequest.class))).thenReturn(response);

        // Act
        var result = restTestClient.post().uri("/api/v1/categories").body(request).exchange();

        // Assert
        result.expectStatus().isCreated()
                .expectHeader().valueEquals("Location", "http://localhost/api/v1/categories/7")
                .expectBody().jsonPath("$.id").isEqualTo(7)
                .jsonPath("$.slug").isEqualTo("audio");
    }

    @Test
    void should_rejectRequest_when_slugIsInvalid() {
        // Arrange
        var request = CreateCategoryRequest.builder().name("Audio").slug("Invalid Slug").build();

        // Act
        var result = restTestClient.post().uri("/api/v1/categories").body(request).exchange();

        // Assert
        result.expectStatus().isBadRequest();
    }
}
