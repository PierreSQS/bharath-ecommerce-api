package com.bharath.ecommerce.api.controller;

import com.bharath.ecommerce.api.dto.CreateReviewRequest;
import com.bharath.ecommerce.api.dto.ReviewResponse;
import com.bharath.ecommerce.api.exception.BusinessRuleException;
import com.bharath.ecommerce.api.service.ReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@WebMvcTest(ReviewController.class)
@AutoConfigureRestTestClient
class ReviewControllerRTClientTest {
    @Autowired private RestTestClient restTestClient;
    @MockitoBean private ReviewService reviewService;

    @Test
    void should_createReview_when_requestIsValid() {
        // Arrange
        var request = CreateReviewRequest.builder().productId(3L).customerId(5L)
                .rating(5).comment("Excellent mouse").build();
        var response = ReviewResponse.builder().id(7L).productId(3L).productName("Wireless Mouse")
                .customerId(5L).customerName("John Doe").rating(5).comment("Excellent mouse").build();
        given(reviewService.create(any(CreateReviewRequest.class))).willReturn(response);

        // Act
        var result = restTestClient.post().uri("/api/v1/reviews").body(request).exchange();

        // Assert
        result.expectStatus().isCreated()
                .expectHeader().valueEquals("Location", "http://localhost/api/v1/reviews/7")
                .expectBody().jsonPath("$.id").isEqualTo(7)
                .jsonPath("$.rating").isEqualTo(5)
                .jsonPath("$.productName").isEqualTo("Wireless Mouse");
    }

    @Test
    void should_returnReviews_when_productIdProvided() {
        // Arrange
        var review = ReviewResponse.builder().id(7L).productId(3L).productName("Wireless Mouse")
                .customerId(5L).customerName("John Doe").rating(4).comment("Solid").build();
        given(reviewService.getReviewsForProduct(3L)).willReturn(List.of(review));

        // Act
        var result = restTestClient.get().uri("/api/v1/reviews?productId=3").exchange();

        // Assert
        result.expectStatus().isOk().expectBody()
                .jsonPath("$[0].id").isEqualTo(7)
                .jsonPath("$[0].rating").isEqualTo(4)
                .jsonPath("$[0].customerName").isEqualTo("John Doe");
    }

    @Test
    void should_rejectRequest_when_ratingExceedsMaximum() {
        // Arrange
        var request = CreateReviewRequest.builder().productId(3L).customerId(5L)
                .rating(6).comment("Out of range").build();
        given(reviewService.create(any(CreateReviewRequest.class)))
                .willThrow(new BusinessRuleException("Rating must be between 1 and 5"));

        // Act
        var result = restTestClient.post().uri("/api/v1/reviews").body(request).exchange();

        // Assert
        result.expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT)
                .expectBody().jsonPath("$.status").isEqualTo(422)
                .jsonPath("$.message").isEqualTo("Rating must be between 1 and 5");
    }

    @Test
    void should_rejectRequest_when_ratingIsMissing() {
        // Arrange
        var request = CreateReviewRequest.builder().productId(3L).customerId(5L)
                .comment("No rating").build();

        // Act
        var result = restTestClient.post().uri("/api/v1/reviews").body(request).exchange();

        // Assert
        result.expectStatus().isBadRequest()
                .expectBody().jsonPath("$.validationErrors.rating").exists();
    }

    @Test
    void should_rejectRequest_when_productIdParameterMissing() {
        // Arrange
        // No stubbing needed: the request never reaches the service.

        // Act
        var result = restTestClient.get().uri("/api/v1/reviews").exchange();

        // Assert
        result.expectStatus().isBadRequest();
    }
}
