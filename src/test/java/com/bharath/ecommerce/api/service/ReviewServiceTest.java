package com.bharath.ecommerce.api.service;

import com.bharath.ecommerce.api.dto.CreateReviewRequest;
import com.bharath.ecommerce.api.entity.Customer;
import com.bharath.ecommerce.api.entity.Product;
import com.bharath.ecommerce.api.entity.Review;
import com.bharath.ecommerce.api.exception.BusinessRuleException;
import com.bharath.ecommerce.api.exception.DuplicateResourceException;
import com.bharath.ecommerce.api.exception.ResourceNotFoundException;
import com.bharath.ecommerce.api.repository.CustomerRepository;
import com.bharath.ecommerce.api.repository.ProductRepository;
import com.bharath.ecommerce.api.repository.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(ReviewService.class)
class ReviewServiceTest {
    @MockitoBean
    private ReviewRepository reviewRepository;
    @MockitoBean
    private ProductRepository productRepository;
    @MockitoBean
    private CustomerRepository customerRepository;

    @Autowired
    private ReviewService service;

    @Test
    void should_createReview_when_productAndCustomerExist() {
        when(productRepository.findById(3L)).thenReturn(Optional.of(product()));
        when(customerRepository.findById(5L)).thenReturn(Optional.of(customer()));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(CreateReviewRequest.builder().productId(3L).customerId(5L)
                .rating(5).comment("  Excellent mouse  ").build());

        assertThat(response.getRating()).isEqualTo(5);
        assertThat(response.getComment()).isEqualTo("Excellent mouse");
        assertThat(response.getProductName()).isEqualTo("Wireless Mouse");
        assertThat(response.getCustomerName()).isEqualTo("John Doe");
    }

    @Test
    void should_createReview_when_ratingIsAtLowerBound() {
        when(productRepository.findById(3L)).thenReturn(Optional.of(product()));
        when(customerRepository.findById(5L)).thenReturn(Optional.of(customer()));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(CreateReviewRequest.builder().productId(3L).customerId(5L)
                .rating(1).build());

        assertThat(response.getRating()).isEqualTo(1);
    }

    @Test
    void should_rejectReview_when_ratingIsBelowMinimum() {
        assertThatThrownBy(() -> service.create(CreateReviewRequest.builder().productId(3L)
                .customerId(5L).rating(0).build()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Rating must be between 1 and 5");
        verify(productRepository, never()).findById(any());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void should_rejectReview_when_ratingExceedsMaximum() {
        assertThatThrownBy(() -> service.create(CreateReviewRequest.builder().productId(3L)
                .customerId(5L).rating(6).build()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Rating must be between 1 and 5");
        verify(productRepository, never()).findById(any());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void should_rejectReview_when_ratingIsMissing() {
        assertThatThrownBy(() -> service.create(CreateReviewRequest.builder().productId(3L)
                .customerId(5L).build()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Rating must be between 1 and 5");
        verify(productRepository, never()).findById(any());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void should_rejectReview_when_productIsUnknown() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(CreateReviewRequest.builder().productId(99L)
                .customerId(5L).rating(5).build()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product not found with id 99");
        verify(customerRepository, never()).findById(any());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void should_rejectReview_when_customerIsUnknown() {
        when(productRepository.findById(3L)).thenReturn(Optional.of(product()));
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(CreateReviewRequest.builder().productId(3L)
                .customerId(99L).rating(5).build()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Customer not found with id 99");
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void should_rejectReview_when_customerAlreadyReviewedProduct() {
        when(productRepository.findById(3L)).thenReturn(Optional.of(product()));
        when(customerRepository.findById(5L)).thenReturn(Optional.of(customer()));
        when(reviewRepository.existsByProductIdAndCustomerId(3L, 5L)).thenReturn(true);

        assertThatThrownBy(() -> service.create(CreateReviewRequest.builder().productId(3L)
                .customerId(5L).rating(4).build()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Customer 5 has already reviewed product 3");
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void should_returnReviews_when_productExists() {
        when(productRepository.existsById(3L)).thenReturn(true);
        when(reviewRepository.findByProductIdOrderByCreatedAtDesc(3L)).thenReturn(List.of(
                Review.builder().id(7L).product(product()).customer(customer()).rating(4).comment("Solid").build()));

        var responses = service.getReviewsForProduct(3L);

        assertThat(responses).singleElement().satisfies(response -> {
            assertThat(response.getId()).isEqualTo(7L);
            assertThat(response.getRating()).isEqualTo(4);
            assertThat(response.getCustomerName()).isEqualTo("John Doe");
        });
    }

    @Test
    void should_rejectListing_when_productIsUnknown() {
        when(productRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.getReviewsForProduct(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product not found with id 99");
        verify(reviewRepository, never()).findByProductIdOrderByCreatedAtDesc(any());
    }

    private Product product() {
        return Product.builder().id(3L).name("Wireless Mouse").build();
    }

    private Customer customer() {
        return Customer.builder().id(5L).firstName("John").lastName("Doe").build();
    }
}
