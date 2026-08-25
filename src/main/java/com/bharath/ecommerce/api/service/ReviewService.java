package com.bharath.ecommerce.api.service;

import com.bharath.ecommerce.api.dto.CreateReviewRequest;
import com.bharath.ecommerce.api.dto.ReviewResponse;
import com.bharath.ecommerce.api.entity.Customer;
import com.bharath.ecommerce.api.entity.Product;
import com.bharath.ecommerce.api.entity.Review;
import com.bharath.ecommerce.api.exception.BusinessRuleException;
import com.bharath.ecommerce.api.exception.DuplicateResourceException;
import com.bharath.ecommerce.api.exception.ResourceNotFoundException;
import com.bharath.ecommerce.api.repository.CustomerRepository;
import com.bharath.ecommerce.api.repository.ProductRepository;
import com.bharath.ecommerce.api.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private static final int MIN_RATING = 1;
    private static final int MAX_RATING = 5;

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;

    /**
     * The uniqueness check is advisory: two concurrent posts can both pass it, and the second
     * insert then trips uk_reviews_product_customer, which maps to the same 409 as this check.
     */
    @Transactional
    public ReviewResponse create(CreateReviewRequest request) {
        Integer rating = request.getRating();
        if (rating == null || rating < MIN_RATING || rating > MAX_RATING) {
            throw new BusinessRuleException(
                    "Rating must be between " + MIN_RATING + " and " + MAX_RATING);
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id " + request.getProductId()));
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer not found with id " + request.getCustomerId()));

        if (reviewRepository.existsByProductIdAndCustomerId(product.getId(), customer.getId())) {
            throw new DuplicateResourceException("Customer " + customer.getId()
                    + " has already reviewed product " + product.getId());
        }

        Review review = Review.builder()
                .product(product)
                .customer(customer)
                .rating(request.getRating())
                .comment(trimToNull(request.getComment()))
                .build();
        return toResponse(reviewRepository.save(review));
    }

    /** Checks the product first so an unknown id is a 404 rather than a misleading empty list. */
    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsForProduct(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found with id " + productId);
        }
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId).stream()
                .map(this::toResponse).toList();
    }

    private ReviewResponse toResponse(Review review) {
        Product product = review.getProduct();
        Customer customer = review.getCustomer();
        return ReviewResponse.builder().id(review.getId())
                .productId(product.getId()).productName(product.getName())
                .customerId(customer.getId())
                .customerName(customer.getFirstName() + " " + customer.getLastName())
                .rating(review.getRating()).comment(review.getComment())
                .createdAt(review.getCreatedAt()).build();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
