package com.bharath.ecommerce.api.exception;

import com.bharath.ecommerce.api.controller.CategoryController;
import com.bharath.ecommerce.api.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
class GlobalExceptionHandlerMvcTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private CategoryService categoryService;

    @Test
    void should_return404_when_pathIsUnmapped() throws Exception {
        // Arrange
        // Act
        var result = mockMvc.perform(get("/favicon.ico"));

        // Assert
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.path").value("/favicon.ico"));
    }
}
