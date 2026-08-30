package com.bharath.ecommerce.api.controller;

import com.bharath.ecommerce.api.dto.CategoryResponse;
import com.bharath.ecommerce.api.dto.CreateCategoryRequest;
import com.bharath.ecommerce.api.exception.DuplicateResourceException;
import com.bharath.ecommerce.api.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
class CategoryControllerMvcTest {

    private static final String CATEGORIES_URI = "/api/v1/categories";

    @MockitoBean
    private CategoryService categoryService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void should_return_201_with_location_and_body_when_category_is_created() throws Exception {
        // Given
        var request = CreateCategoryRequest.builder().name("Home Office").slug("home-office")
                .description("Desks and chairs").build();
        var response = CategoryResponse.builder().id(7L).name("Home Office").slug("home-office")
                .description("Desks and chairs").createdAt(LocalDateTime.of(2026, 1, 2, 3, 4))
                .updatedAt(LocalDateTime.of(2026, 1, 2, 3, 4)).build();
        given(categoryService.create(any(CreateCategoryRequest.class))).willReturn(response);

        // When
        var result = mockMvc.perform(post(CATEGORIES_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)));

        // Then
        result.andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost" + CATEGORIES_URI + "/7"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.name").value("Home Office"))
                .andExpect(jsonPath("$.slug").value("home-office"))
                .andExpect(jsonPath("$.description").value("Desks and chairs"));
        then(categoryService).should().create(any(CreateCategoryRequest.class));
    }

    @Test
    void should_return_400_with_all_field_errors_when_request_is_invalid() throws Exception {
        // Given
        var request = CreateCategoryRequest.builder().name("  ").slug("Home Office!").build();

        // When
        var result = mockMvc.perform(post(CATEGORIES_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)));

        // Then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.path").value(CATEGORIES_URI))
                .andExpect(jsonPath("$.validationErrors.name").exists())
                .andExpect(jsonPath("$.validationErrors.slug")
                        .value("must contain lowercase letters, numbers, and single hyphens only"));
        then(categoryService).should(never()).create(any());
    }

    @Test
    void should_return_409_when_category_name_already_exists() throws Exception {
        // Given
        var request = CreateCategoryRequest.builder().name("Home Office").slug("home-office").build();
        given(categoryService.create(any(CreateCategoryRequest.class)))
                .willThrow(new DuplicateResourceException("Category name already exists: Home Office"));

        // When
        var result = mockMvc.perform(post(CATEGORIES_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)));

        // Then
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Category name already exists: Home Office"))
                .andExpect(jsonPath("$.path").value(CATEGORIES_URI))
                .andExpect(jsonPath("$.validationErrors").doesNotExist());
        then(categoryService).should().create(any(CreateCategoryRequest.class));
    }
}
