package com.bharath.ecommerce.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

    @NotNull @Min(1) @Max(5)
    private Integer rating;

    @Size(max = 2000)
    private String comment;
}
