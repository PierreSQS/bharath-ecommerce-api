package com.bharath.ecommerce.api.dto;

import com.bharath.ecommerce.api.entity.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreateOrderRequest {
    @NotNull @Positive private Long customerId;
    @NotBlank private String shippingAddress;
    private String notes;
    @NotNull private PaymentMethod paymentMethod;
    @NotEmpty private List<@Valid OrderItemRequest> items;
}
