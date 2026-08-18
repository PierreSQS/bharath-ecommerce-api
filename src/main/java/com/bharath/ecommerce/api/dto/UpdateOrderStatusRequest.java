package com.bharath.ecommerce.api.dto;

import com.bharath.ecommerce.api.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UpdateOrderStatusRequest {
    @NotNull private OrderStatus status;
}
