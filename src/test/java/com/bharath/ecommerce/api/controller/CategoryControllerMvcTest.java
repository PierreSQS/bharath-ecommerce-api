package com.bharath.ecommerce.api.controller;

import com.bharath.ecommerce.api.dto.CategoryResponse;
import com.bharath.ecommerce.api.dto.CreateCategoryRequest;
import com.bharath.ecommerce.api.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
class CategoryControllerMvcTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper jsonMapper;
    @MockitoBean private CategoryService categoryService;

    @Test
    void should_createCategory_when_requestIsValid() throws Exception {
        // Arrange
        var request = CreateCategoryRequest.builder().name("Audio").slug("audio").description("Audio gear").build();
        var response = CategoryResponse.builder().id(7L).name("Audio").slug("audio").description("Audio gear").build();
        when(categoryService.create(any(CreateCategoryRequest.class))).thenReturn(response);

        // Act
        var result = mockMvc.perform(post("/api/v1/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)));

        // Assert
        result.andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/categories/7"))
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.slug").value("audio"));
    }

    @Test
    void should_rejectRequest_when_slugIsInvalid() throws Exception {
        // Arrange
        var request = CreateCategoryRequest.builder().name("Audio").slug("Invalid Slug").build();

        // Act
        var result = mockMvc.perform(post("/api/v1/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)));

        // Assert
        result.andExpect(status().isBadRequest());
    }
}
