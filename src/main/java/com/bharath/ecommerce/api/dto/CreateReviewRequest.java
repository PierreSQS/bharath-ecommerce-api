package com.bharath.ecommerce.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreateReviewRequest {
    @NotNull @Positive
    private Long productId;

    @NotNull @Positive
    private Long customerId;

    /** Range is enforced in ReviewService as a business rule, so it applies to every caller. */
    @NotNull
    private Integer rating;

    @Size(max = 2000)
    private String comment;
}
