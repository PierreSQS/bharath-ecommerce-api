package com.bharath.ecommerce.api.controller;

import com.bharath.ecommerce.api.dto.CreateReviewRequest;
import com.bharath.ecommerce.api.dto.ReviewResponse;
import com.bharath.ecommerce.api.service.ReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReviewController.class)
class ReviewControllerMvcTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper jsonMapper;
    @MockitoBean private ReviewService reviewService;

    @Test
    void should_createReview_when_requestIsValid() throws Exception {
        // Arrange
        var request = CreateReviewRequest.builder().productId(3L).customerId(5L)
                .rating(5).comment("Excellent mouse").build();
        var response = ReviewResponse.builder().id(7L).productId(3L).productName("Wireless Mouse")
                .customerId(5L).customerName("John Doe").rating(5).comment("Excellent mouse").build();
        when(reviewService.create(any(CreateReviewRequest.class))).thenReturn(response);

        // Act
        var result = mockMvc.perform(post("/api/v1/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)));

        // Assert
        result.andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/reviews/7"))
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.productName").value("Wireless Mouse"))
                .andDo(print());
    }

    @Test
    void should_returnReviews_when_productIdProvided() throws Exception {
        // Arrange
        var review = ReviewResponse.builder().id(7L).productId(3L).productName("Wireless Mouse")
                .customerId(5L).customerName("John Doe").rating(4).comment("Solid").build();
        when(reviewService.getReviewsForProduct(3L)).thenReturn(List.of(review));

        // Act
        var result = mockMvc.perform(get("/api/v1/reviews").param("productId", "3"));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].rating").value(4))
                .andExpect(jsonPath("$[0].customerName").value("John Doe"));
    }

    @Test
    void should_rejectRequest_when_ratingExceedsMaximum() throws Exception {
        // Arrange
        var request = CreateReviewRequest.builder().productId(3L).customerId(5L)
                .rating(6).comment("Out of range").build();

        // Act
        var result = mockMvc.perform(post("/api/v1/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)));

        // Assert
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.rating").exists());
    }

    @Test
    void should_rejectRequest_when_productIdParameterMissing() throws Exception {
        // Arrange
        // No stubbing needed: the request never reaches the service.

        // Act
        var result = mockMvc.perform(get("/api/v1/reviews"));

        // Assert
        result.andExpect(status().isBadRequest());
    }
}
