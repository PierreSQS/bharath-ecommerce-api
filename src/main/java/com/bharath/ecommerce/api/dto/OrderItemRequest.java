package com.bharath.ecommerce.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class OrderItemRequest {
    @NotNull @Positive private Long productId;
    @NotNull @Positive private Integer quantity;
}
