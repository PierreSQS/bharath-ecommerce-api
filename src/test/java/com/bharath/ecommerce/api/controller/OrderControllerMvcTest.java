package com.bharath.ecommerce.api.controller;

import com.bharath.ecommerce.api.dto.CreateOrderRequest;
import com.bharath.ecommerce.api.dto.OrderItemRequest;
import com.bharath.ecommerce.api.dto.OrderItemResponse;
import com.bharath.ecommerce.api.dto.OrderResponse;
import com.bharath.ecommerce.api.dto.PaymentResponse;
import com.bharath.ecommerce.api.dto.UpdateOrderStatusRequest;
import com.bharath.ecommerce.api.entity.OrderStatus;
import com.bharath.ecommerce.api.entity.PaymentMethod;
import com.bharath.ecommerce.api.entity.PaymentStatus;
import com.bharath.ecommerce.api.exception.BusinessRuleException;
import com.bharath.ecommerce.api.exception.DuplicateResourceException;
import com.bharath.ecommerce.api.exception.InsufficientStockException;
import com.bharath.ecommerce.api.exception.InvalidOrderStatusTransitionException;
import com.bharath.ecommerce.api.exception.ResourceNotFoundException;
import com.bharath.ecommerce.api.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerMvcTest {

    private static final String ORDERS_URI = "/api/v1/orders";

    @MockitoBean
    private OrderService orderService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void should_return_200_with_all_orders_when_orders_are_listed() throws Exception {
        // Given
        given(orderService.getAllOrders()).willReturn(List.of(
                orderResponse(1L, "ORD-0001", OrderStatus.PENDING),
                orderResponse(2L, "ORD-0002", OrderStatus.SHIPPED)));

        // When
        var result = mockMvc.perform(get(ORDERS_URI));

        // Then
        result.andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].orderNumber").value("ORD-0001"))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[1].orderNumber").value("ORD-0002"))
                .andExpect(jsonPath("$[1].status").value("SHIPPED"));
        then(orderService).should().getAllOrders();
    }

    @Test
    void should_return_201_with_location_and_body_when_order_is_placed() throws Exception {
        // Given
        given(orderService.placeOrder(any(CreateOrderRequest.class)))
                .willReturn(orderResponse(5L, "ORD-0005", OrderStatus.PENDING));

        // When
        var result = mockMvc.perform(post(ORDERS_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(validRequest())));

        // Then
        result.andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost" + ORDERS_URI + "/5"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.orderNumber").value("ORD-0005"))
                .andExpect(jsonPath("$.customerId").value(11))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].productId").value(3))
                .andExpect(jsonPath("$.payment.paymentMethod").value("CREDIT_CARD"));
        then(orderService).should().placeOrder(any(CreateOrderRequest.class));
    }

    @Test
    void should_return_400_with_all_field_errors_when_order_request_is_invalid() throws Exception {
        // Given
        var request = CreateOrderRequest.builder().customerId(null).shippingAddress("  ")
                .paymentMethod(null).items(List.of()).build();

        // When
        var result = mockMvc.perform(post(ORDERS_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)));

        // Then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.path").value(ORDERS_URI))
                .andExpect(jsonPath("$.validationErrors.customerId").exists())
                .andExpect(jsonPath("$.validationErrors.shippingAddress").exists())
                .andExpect(jsonPath("$.validationErrors.paymentMethod").exists())
                .andExpect(jsonPath("$.validationErrors.items").exists());
        then(orderService).should(never()).placeOrder(any());
    }

    @Test
    void should_return_400_with_nested_field_errors_when_order_item_is_invalid() throws Exception {
        // Given
        var request = CreateOrderRequest.builder().customerId(11L).shippingAddress("12 Analytical Street")
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .items(List.of(OrderItemRequest.builder().productId(0L).quantity(-2).build())).build();

        // When
        var result = mockMvc.perform(post(ORDERS_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)));

        // Then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.validationErrors['items[0].productId']").exists())
                .andExpect(jsonPath("$.validationErrors['items[0].quantity']").exists());
        then(orderService).should(never()).placeOrder(any());
    }

    @Test
    void should_return_400_when_payment_method_is_not_a_known_enum_value() throws Exception {
        // Given
        var body = """
                {"customerId":11,"shippingAddress":"12 Analytical Street","paymentMethod":"BITCOIN",
                 "items":[{"productId":3,"quantity":2}]}
                """;

        // When
        var result = mockMvc.perform(post(ORDERS_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // Then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Malformed request body or unsupported enum value"))
                .andExpect(jsonPath("$.path").value(ORDERS_URI))
                .andExpect(jsonPath("$.validationErrors").doesNotExist());
        then(orderService).should(never()).placeOrder(any());
    }

    @Test
    void should_return_422_when_placing_an_order_violates_a_business_rule() throws Exception {
        // Given
        given(orderService.placeOrder(any(CreateOrderRequest.class)))
                .willThrow(new BusinessRuleException("Product 3 is not available for ordering"));

        // When
        var result = mockMvc.perform(post(ORDERS_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(validRequest())));

        // Then
        result.andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message").value("Product 3 is not available for ordering"))
                .andExpect(jsonPath("$.path").value(ORDERS_URI))
                .andExpect(jsonPath("$.validationErrors").doesNotExist());
        then(orderService).should().placeOrder(any(CreateOrderRequest.class));
    }

    @Test
    void should_return_404_when_a_product_of_the_order_does_not_exist() throws Exception {
        // Given
        given(orderService.placeOrder(any(CreateOrderRequest.class)))
                .willThrow(new ResourceNotFoundException("Product not found with id 3"));

        // When
        var result = mockMvc.perform(post(ORDERS_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(validRequest())));

        // Then
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Product not found with id 3"))
                .andExpect(jsonPath("$.path").value(ORDERS_URI));
        then(orderService).should().placeOrder(any(CreateOrderRequest.class));
    }

    @Test
    void should_return_409_when_stock_is_insufficient_for_the_order() throws Exception {
        // Given
        given(orderService.placeOrder(any(CreateOrderRequest.class)))
                .willThrow(new InsufficientStockException("Insufficient stock for product 3"));

        // When
        var result = mockMvc.perform(post(ORDERS_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(validRequest())));

        // Then
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Insufficient stock for product 3"))
                .andExpect(jsonPath("$.path").value(ORDERS_URI));
        then(orderService).should().placeOrder(any(CreateOrderRequest.class));
    }

    @Test
    void should_return_200_with_the_order_when_it_is_fetched_by_id() throws Exception {
        // Given
        given(orderService.getOrder(5L)).willReturn(orderResponse(5L, "ORD-0005", OrderStatus.CONFIRMED));

        // When
        var result = mockMvc.perform(get(ORDERS_URI + "/{id}", 5L));

        // Then
        result.andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.orderNumber").value("ORD-0005"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.totalAmount").value(499.98));
        then(orderService).should().getOrder(5L);
    }

    @Test
    void should_return_404_when_the_order_id_does_not_exist() throws Exception {
        // Given
        given(orderService.getOrder(404L)).willThrow(new ResourceNotFoundException("Order not found with id 404"));

        // When
        var result = mockMvc.perform(get(ORDERS_URI + "/{id}", 404L));

        // Then
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Order not found with id 404"))
                .andExpect(jsonPath("$.path").value(ORDERS_URI + "/404"));
        then(orderService).should().getOrder(404L);
    }

    @Test
    void should_return_200_with_the_order_when_it_is_fetched_by_order_number() throws Exception {
        // Given
        given(orderService.getOrderByNumber("ORD-0005"))
                .willReturn(orderResponse(5L, "ORD-0005", OrderStatus.PENDING));

        // When
        var result = mockMvc.perform(get(ORDERS_URI + "/number/{orderNumber}", "ORD-0005"));

        // Then
        result.andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.orderNumber").value("ORD-0005"))
                .andExpect(jsonPath("$.customerName").value("Ada Lovelace"));
        then(orderService).should().getOrderByNumber("ORD-0005");
    }

    @Test
    void should_return_404_when_the_order_number_does_not_exist() throws Exception {
        // Given
        given(orderService.getOrderByNumber("ORD-9999"))
                .willThrow(new ResourceNotFoundException("Order not found with number ORD-9999"));

        // When
        var result = mockMvc.perform(get(ORDERS_URI + "/number/{orderNumber}", "ORD-9999"));

        // Then
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Order not found with number ORD-9999"))
                .andExpect(jsonPath("$.path").value(ORDERS_URI + "/number/ORD-9999"));
        then(orderService).should().getOrderByNumber("ORD-9999");
    }

    @Test
    void should_return_200_with_the_updated_order_when_status_is_changed() throws Exception {
        // Given
        var request = UpdateOrderStatusRequest.builder().status(OrderStatus.CONFIRMED).build();
        given(orderService.updateStatus(5L, OrderStatus.CONFIRMED))
                .willReturn(orderResponse(5L, "ORD-0005", OrderStatus.CONFIRMED));

        // When
        var result = mockMvc.perform(patch(ORDERS_URI + "/{id}/status", 5L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)));

        // Then
        result.andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
        then(orderService).should().updateStatus(5L, OrderStatus.CONFIRMED);
    }

    @Test
    void should_return_400_when_the_requested_status_is_missing() throws Exception {
        // Given
        var request = UpdateOrderStatusRequest.builder().status(null).build();

        // When
        var result = mockMvc.perform(patch(ORDERS_URI + "/{id}/status", 5L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)));

        // Then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.path").value(ORDERS_URI + "/5/status"))
                .andExpect(jsonPath("$.validationErrors.status").exists());
        then(orderService).should(never()).updateStatus(any(), any());
    }

    @Test
    void should_return_409_when_the_status_transition_is_not_allowed() throws Exception {
        // Given
        var request = UpdateOrderStatusRequest.builder().status(OrderStatus.DELIVERED).build();
        given(orderService.updateStatus(5L, OrderStatus.DELIVERED))
                .willThrow(new InvalidOrderStatusTransitionException(OrderStatus.PENDING, OrderStatus.DELIVERED));

        // When
        var result = mockMvc.perform(patch(ORDERS_URI + "/{id}/status", 5L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)));

        // Then
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Order status cannot transition from PENDING to DELIVERED"))
                .andExpect(jsonPath("$.path").value(ORDERS_URI + "/5/status"));
        then(orderService).should().updateStatus(5L, OrderStatus.DELIVERED);
    }

    @Test
    void should_return_404_when_updating_the_status_of_an_unknown_order() throws Exception {
        // Given
        var request = UpdateOrderStatusRequest.builder().status(OrderStatus.CONFIRMED).build();
        given(orderService.updateStatus(404L, OrderStatus.CONFIRMED))
                .willThrow(new ResourceNotFoundException("Order not found with id 404"));

        // When
        var result = mockMvc.perform(patch(ORDERS_URI + "/{id}/status", 404L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)));

        // Then
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Order not found with id 404"))
                .andExpect(jsonPath("$.path").value(ORDERS_URI + "/404/status"));
        then(orderService).should().updateStatus(404L, OrderStatus.CONFIRMED);
    }

    @Test
    void should_return_200_with_the_cancelled_order_when_it_is_cancelled() throws Exception {
        // Given
        given(orderService.cancelOrder(5L)).willReturn(orderResponse(5L, "ORD-0005", OrderStatus.CANCELLED));

        // When
        var result = mockMvc.perform(post(ORDERS_URI + "/{id}/cancel", 5L));

        // Then
        result.andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        then(orderService).should().cancelOrder(5L);
    }

    @Test
    void should_return_409_when_the_order_is_already_cancelled() throws Exception {
        // Given
        given(orderService.cancelOrder(5L))
                .willThrow(new DuplicateResourceException("Order ORD-0005 is already cancelled"));

        // When
        var result = mockMvc.perform(post(ORDERS_URI + "/{id}/cancel", 5L));

        // Then
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Order ORD-0005 is already cancelled"))
                .andExpect(jsonPath("$.path").value(ORDERS_URI + "/5/cancel"));
        then(orderService).should().cancelOrder(5L);
    }

    @Test
    void should_return_404_when_cancelling_an_unknown_order() throws Exception {
        // Given
        given(orderService.cancelOrder(404L)).willThrow(new ResourceNotFoundException("Order not found with id 404"));

        // When
        var result = mockMvc.perform(post(ORDERS_URI + "/{id}/cancel", 404L));

        // Then
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Order not found with id 404"))
                .andExpect(jsonPath("$.path").value(ORDERS_URI + "/404/cancel"));
        then(orderService).should().cancelOrder(404L);
    }

    private CreateOrderRequest validRequest() {
        return CreateOrderRequest.builder().customerId(11L).shippingAddress("12 Analytical Street")
                .notes("Leave at the door").paymentMethod(PaymentMethod.CREDIT_CARD)
                .items(List.of(OrderItemRequest.builder().productId(3L).quantity(2).build())).build();
    }

    private OrderResponse orderResponse(Long id, String orderNumber, OrderStatus orderStatus) {
        return OrderResponse.builder().id(id).orderNumber(orderNumber).customerId(11L)
                .customerName("Ada Lovelace").status(orderStatus).totalAmount(new BigDecimal("499.98"))
                .shippingAddress("12 Analytical Street").notes("Leave at the door")
                .createdAt(LocalDateTime.of(2026, 5, 6, 7, 8)).updatedAt(LocalDateTime.of(2026, 5, 6, 7, 8))
                .items(List.of(OrderItemResponse.builder().id(1L).productId(3L).productName("Standing Desk")
                        .sku("DESK-001").quantity(2).unitPrice(new BigDecimal("249.99"))
                        .subtotal(new BigDecimal("499.98")).build()))
                .payment(PaymentResponse.builder().id(1L).paymentMethod(PaymentMethod.CREDIT_CARD)
                        .paymentStatus(PaymentStatus.PENDING).amount(new BigDecimal("499.98"))
                        .createdAt(LocalDateTime.of(2026, 5, 6, 7, 8)).build())
                .build();
    }
}
