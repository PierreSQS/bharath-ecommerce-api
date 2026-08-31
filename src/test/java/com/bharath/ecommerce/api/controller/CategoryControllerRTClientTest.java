package com.bharath.ecommerce.api.controller;

import com.bharath.ecommerce.api.dto.CategoryResponse;
import com.bharath.ecommerce.api.dto.CreateCategoryRequest;
import com.bharath.ecommerce.api.exception.DuplicateResourceException;
import com.bharath.ecommerce.api.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;

@WebMvcTest(CategoryController.class)
class CategoryControllerRTClientTest {

    private static final String CATEGORIES_URI = "/api/v1/categories";

    @MockitoBean
    private CategoryService categoryService;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JsonMapper jsonMapper;

    private RestTestClient restTestClient;

    @BeforeEach
    void setUp() {
        restTestClient = RestTestClient.bindToApplicationContext(webApplicationContext).build();
    }

    @Test
    void should_return_201_with_location_and_body_when_category_is_created() {
        // Given
        var request = CreateCategoryRequest.builder().name("Home Office").slug("home-office")
                .description("Desks and chairs").build();
        var response = CategoryResponse.builder().id(7L).name("Home Office").slug("home-office")
                .description("Desks and chairs").createdAt(LocalDateTime.of(2026, 1, 2, 3, 4))
                .updatedAt(LocalDateTime.of(2026, 1, 2, 3, 4)).build();
        given(categoryService.create(any(CreateCategoryRequest.class))).willReturn(response);

        // When
        var result = restTestClient.post().uri(CATEGORIES_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .body(jsonMapper.writeValueAsString(request))
                .exchange();

        // Then
        result.expectStatus().isCreated()
                .expectHeader().valueEquals("Location", "http://localhost" + CATEGORIES_URI + "/7")
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.id").isEqualTo(7)
                .jsonPath("$.name").isEqualTo("Home Office")
                .jsonPath("$.slug").isEqualTo("home-office")
                .jsonPath("$.description").isEqualTo("Desks and chairs");
        then(categoryService).should().create(any(CreateCategoryRequest.class));
    }

    @Test
    void should_return_400_with_all_field_errors_when_request_is_invalid() {
        // Given
        var request = CreateCategoryRequest.builder().name("  ").slug("Home Office!").build();

        // When
        var result = restTestClient.post().uri(CATEGORIES_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .body(jsonMapper.writeValueAsString(request))
                .exchange();

        // Then
        result.expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.error").isEqualTo("Bad Request")
                .jsonPath("$.message").isEqualTo("Request validation failed")
                .jsonPath("$.path").isEqualTo(CATEGORIES_URI)
                .jsonPath("$.validationErrors.name").exists()
                .jsonPath("$.validationErrors.slug")
                .isEqualTo("must contain lowercase letters, numbers, and single hyphens only");
        then(categoryService).should(never()).create(any());
    }

    @Test
    void should_return_400_when_request_body_is_malformed() {
        // Given
        var malformedJson = "{\"name\": \"Home Office\", ";

        // When
        var result = restTestClient.post().uri(CATEGORIES_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .body(malformedJson)
                .exchange();

        // Then
        result.expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.error").isEqualTo("Bad Request")
                .jsonPath("$.message").isEqualTo("Malformed request body or unsupported enum value")
                .jsonPath("$.path").isEqualTo(CATEGORIES_URI)
                .jsonPath("$.validationErrors").doesNotExist();
        then(categoryService).should(never()).create(any());
    }

    @Test
    void should_return_409_when_category_name_already_exists() {
        // Given
        var request = CreateCategoryRequest.builder().name("Home Office").slug("home-office").build();
        given(categoryService.create(any(CreateCategoryRequest.class)))
                .willThrow(new DuplicateResourceException("Category name already exists: Home Office"));

        // When
        var result = restTestClient.post().uri(CATEGORIES_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .body(jsonMapper.writeValueAsString(request))
                .exchange();

        // Then
        result.expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody()
                .jsonPath("$.status").isEqualTo(409)
                .jsonPath("$.error").isEqualTo("Conflict")
                .jsonPath("$.message").isEqualTo("Category name already exists: Home Office")
                .jsonPath("$.path").isEqualTo(CATEGORIES_URI)
                .jsonPath("$.validationErrors").doesNotExist();
        then(categoryService).should().create(any(CreateCategoryRequest.class));
    }
}
