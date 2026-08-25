package com.bharath.ecommerce.api.repository;

import com.bharath.ecommerce.api.entity.Review;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    // The response denormalizes both parent names, so fetch them with the listing.
    @EntityGraph(attributePaths = {"product", "customer"})
    List<Review> findByProductIdOrderByCreatedAtDesc(Long productId);

    boolean existsByProductIdAndCustomerId(Long productId, Long customerId);
}
