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
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;

@WebMvcTest(OrderController.class)
@AutoConfigureRestTestClient
class OrderControllerRTClientTest {

    private static final String ORDERS_URI = "/api/v1/orders";

    @MockitoBean
    private OrderService orderService;

    @Autowired
    private RestTestClient restTestClient;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void should_return_200_with_all_orders_when_orders_are_listed() {
        // Given
        given(orderService.getAllOrders()).willReturn(List.of(
                orderResponse(1L, "ORD-0001", OrderStatus.PENDING),
                orderResponse(2L, "ORD-0002", OrderStatus.SHIPPED)));

        // When
        var response = restTestClient.get().uri(ORDERS_URI).exchange();

        // Then
        response.expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].id").isEqualTo(1)
                .jsonPath("$[0].orderNumber").isEqualTo("ORD-0001")
                .jsonPath("$[0].status").isEqualTo("PENDING")
                .jsonPath("$[1].orderNumber").isEqualTo("ORD-0002")
                .jsonPath("$[1].status").isEqualTo("SHIPPED");
        then(orderService).should().getAllOrders();
    }

    @Test
    void should_return_201_with_location_and_body_when_order_is_placed() {
        // Given
        given(orderService.placeOrder(any(CreateOrderRequest.class)))
                .willReturn(orderResponse(5L, "ORD-0005", OrderStatus.PENDING));

        // When
        var response = restTestClient.post().uri(ORDERS_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .body(jsonMapper.writeValueAsString(validRequest()))
                .exchange();

        // Then
        response.expectStatus().isCreated()
                .expectHeader().valueEquals("Location", "http://localhost" + ORDERS_URI + "/5")
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.id").isEqualTo(5)
                .jsonPath("$.orderNumber").isEqualTo("ORD-0005")
                .jsonPath("$.customerId").isEqualTo(11)
                .jsonPath("$.status").isEqualTo("PENDING")
                .jsonPath("$.items.length()").isEqualTo(1)
                .jsonPath("$.items[0].productId").isEqualTo(3)
                .jsonPath("$.payment.paymentMethod").isEqualTo("CREDIT_CARD");
        then(orderService).should().placeOrder(any(CreateOrderRequest.class));
    }

    @Test
    void should_return_400_with_all_field_errors_when_order_request_is_invalid() {
        // Given
        var request = CreateOrderRequest.builder().customerId(null).shippingAddress("  ")
                .paymentMethod(null).items(List.of()).build();

        // When
        var response = restTestClient.post().uri(ORDERS_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .body(jsonMapper.writeValueAsString(request))
                .exchange();

        // Then
        response.expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.error").isEqualTo("Bad Request")
                .jsonPath("$.message").isEqualTo("Request validation failed")
                .jsonPath("$.path").isEqualTo(ORDERS_URI)
                .jsonPath("$.validationErrors.customerId").exists()
                .jsonPath("$.validationErrors.shippingAddress").exists()
                .jsonPath("$.validationErrors.paymentMethod").exists()
                .jsonPath("$.validationErrors.items").exists();
        then(orderService).should(never()).placeOrder(any());
    }

    @Test
    void should_return_400_with_nested_field_errors_when_order_item_is_invalid() {
        // Given
        var request = CreateOrderRequest.builder().customerId(11L).shippingAddress("12 Analytical Street")
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .items(List.of(OrderItemRequest.builder().productId(0L).quantity(-2).build())).build();

        // When
        var response = restTestClient.post().uri(ORDERS_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .body(jsonMapper.writeValueAsString(request))
                .exchange();

        // Then
        response.expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.message").isEqualTo("Request validation failed")
                .jsonPath("$.validationErrors['items[0].productId']").exists()
                .jsonPath("$.validationErrors['items[0].quantity']").exists();
        then(orderService).should(never()).placeOrder(any());
    }

    @Test
    void should_return_400_when_payment_method_is_not_a_known_enum_value() {
        // Given
        var body = """
                {"customerId":11,"shippingAddress":"12 Analytical Street","paymentMethod":"BITCOIN",
                 "items":[{"productId":3,"quantity":2}]}
                """;

        // When
        var response = restTestClient.post().uri(ORDERS_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .exchange();

        // Then
        response.expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.message").isEqualTo("Malformed request body or unsupported enum value")
                .jsonPath("$.path").isEqualTo(ORDERS_URI)
                .jsonPath("$.validationErrors").doesNotExist();
        then(orderService).should(never()).placeOrder(any());
    }

    @Test
    void should_return_422_when_placing_an_order_violates_a_business_rule() {
        // Given
        given(orderService.placeOrder(any(CreateOrderRequest.class)))
                .willThrow(new BusinessRuleException("Product 3 is not available for ordering"));

        // When
        var response = restTestClient.post().uri(ORDERS_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .body(jsonMapper.writeValueAsString(validRequest()))
                .exchange();

        // Then
        response.expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT)
                .expectBody()
                .jsonPath("$.status").isEqualTo(422)
                .jsonPath("$.message").isEqualTo("Product 3 is not available for ordering")
                .jsonPath("$.path").isEqualTo(ORDERS_URI)
                .jsonPath("$.validationErrors").doesNotExist();
        then(orderService).should().placeOrder(any(CreateOrderRequest.class));
    }

    @Test
    void should_return_404_when_a_product_of_the_order_does_not_exist() {
        // Given
        given(orderService.placeOrder(any(CreateOrderRequest.class)))
                .willThrow(new ResourceNotFoundException("Product not found with id 3"));

        // When
        var response = restTestClient.post().uri(ORDERS_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .body(jsonMapper.writeValueAsString(validRequest()))
                .exchange();

        // Then
        response.expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.error").isEqualTo("Not Found")
                .jsonPath("$.message").isEqualTo("Product not found with id 3")
                .jsonPath("$.path").isEqualTo(ORDERS_URI);
        then(orderService).should().placeOrder(any(CreateOrderRequest.class));
    }

    @Test
    void should_return_409_when_stock_is_insufficient_for_the_order() {
        // Given
        given(orderService.placeOrder(any(CreateOrderRequest.class)))
                .willThrow(new InsufficientStockException("Insufficient stock for product 3"));

        // When
        var response = restTestClient.post().uri(ORDERS_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .body(jsonMapper.writeValueAsString(validRequest()))
                .exchange();

        // Then
        response.expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody()
                .jsonPath("$.status").isEqualTo(409)
                .jsonPath("$.error").isEqualTo("Conflict")
                .jsonPath("$.message").isEqualTo("Insufficient stock for product 3")
                .jsonPath("$.path").isEqualTo(ORDERS_URI);
        then(orderService).should().placeOrder(any(CreateOrderRequest.class));
    }

    @Test
    void should_return_200_with_the_order_when_it_is_fetched_by_id() {
        // Given
        given(orderService.getOrder(5L)).willReturn(orderResponse(5L, "ORD-0005", OrderStatus.CONFIRMED));

        // When
        var response = restTestClient.get().uri(ORDERS_URI + "/{id}", 5L).exchange();

        // Then
        response.expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.id").isEqualTo(5)
                .jsonPath("$.orderNumber").isEqualTo("ORD-0005")
                .jsonPath("$.status").isEqualTo("CONFIRMED")
                .jsonPath("$.totalAmount").isEqualTo(499.98);
        then(orderService).should().getOrder(5L);
    }

    @Test
    void should_return_404_when_the_order_id_does_not_exist() {
        // Given
        given(orderService.getOrder(404L)).willThrow(new ResourceNotFoundException("Order not found with id 404"));

        // When
        var response = restTestClient.get().uri(ORDERS_URI + "/{id}", 404L).exchange();

        // Then
        response.expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.error").isEqualTo("Not Found")
                .jsonPath("$.message").isEqualTo("Order not found with id 404")
                .jsonPath("$.path").isEqualTo(ORDERS_URI + "/404");
        then(orderService).should().getOrder(404L);
    }

    @Test
    void should_return_200_with_the_order_when_it_is_fetched_by_order_number() {
        // Given
        given(orderService.getOrderByNumber("ORD-0005"))
                .willReturn(orderResponse(5L, "ORD-0005", OrderStatus.PENDING));

        // When
        var response = restTestClient.get().uri(ORDERS_URI + "/number/{orderNumber}", "ORD-0005").exchange();

        // Then
        response.expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.id").isEqualTo(5)
                .jsonPath("$.orderNumber").isEqualTo("ORD-0005")
                .jsonPath("$.customerName").isEqualTo("Ada Lovelace");
        then(orderService).should().getOrderByNumber("ORD-0005");
    }

    @Test
    void should_return_404_when_the_order_number_does_not_exist() {
        // Given
        given(orderService.getOrderByNumber("ORD-9999"))
                .willThrow(new ResourceNotFoundException("Order not found with number ORD-9999"));

        // When
        var response = restTestClient.get().uri(ORDERS_URI + "/number/{orderNumber}", "ORD-9999").exchange();

        // Then
        response.expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.message").isEqualTo("Order not found with number ORD-9999")
                .jsonPath("$.path").isEqualTo(ORDERS_URI + "/number/ORD-9999");
        then(orderService).should().getOrderByNumber("ORD-9999");
    }

    @Test
    void should_return_200_with_the_updated_order_when_status_is_changed() {
        // Given
        var request = UpdateOrderStatusRequest.builder().status(OrderStatus.CONFIRMED).build();
        given(orderService.updateStatus(5L, OrderStatus.CONFIRMED))
                .willReturn(orderResponse(5L, "ORD-0005", OrderStatus.CONFIRMED));

        // When
        var response = restTestClient.patch().uri(ORDERS_URI + "/{id}/status", 5L)
                .contentType(MediaType.APPLICATION_JSON)
                .body(jsonMapper.writeValueAsString(request))
                .exchange();

        // Then
        response.expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.id").isEqualTo(5)
                .jsonPath("$.status").isEqualTo("CONFIRMED");
        then(orderService).should().updateStatus(5L, OrderStatus.CONFIRMED);
    }

    @Test
    void should_return_400_when_the_requested_status_is_missing() {
        // Given
        var request = UpdateOrderStatusRequest.builder().status(null).build();

        // When
        var response = restTestClient.patch().uri(ORDERS_URI + "/{id}/status", 5L)
                .contentType(MediaType.APPLICATION_JSON)
                .body(jsonMapper.writeValueAsString(request))
                .exchange();

        // Then
        response.expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.message").isEqualTo("Request validation failed")
                .jsonPath("$.path").isEqualTo(ORDERS_URI + "/5/status")
                .jsonPath("$.validationErrors.status").exists();
        then(orderService).should(never()).updateStatus(any(), any());
    }

    @Test
    void should_return_409_when_the_status_transition_is_not_allowed() {
        // Given
        var request = UpdateOrderStatusRequest.builder().status(OrderStatus.DELIVERED).build();
        given(orderService.updateStatus(5L, OrderStatus.DELIVERED))
                .willThrow(new InvalidOrderStatusTransitionException(OrderStatus.PENDING, OrderStatus.DELIVERED));

        // When
        var response = restTestClient.patch().uri(ORDERS_URI + "/{id}/status", 5L)
                .contentType(MediaType.APPLICATION_JSON)
                .body(jsonMapper.writeValueAsString(request))
                .exchange();

        // Then
        response.expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody()
                .jsonPath("$.status").isEqualTo(409)
                .jsonPath("$.error").isEqualTo("Conflict")
                .jsonPath("$.message").isEqualTo("Order status cannot transition from PENDING to DELIVERED")
                .jsonPath("$.path").isEqualTo(ORDERS_URI + "/5/status");
        then(orderService).should().updateStatus(5L, OrderStatus.DELIVERED);
    }

    @Test
    void should_return_404_when_updating_the_status_of_an_unknown_order() {
        // Given
        var request = UpdateOrderStatusRequest.builder().status(OrderStatus.CONFIRMED).build();
        given(orderService.updateStatus(404L, OrderStatus.CONFIRMED))
                .willThrow(new ResourceNotFoundException("Order not found with id 404"));

        // When
        var response = restTestClient.patch().uri(ORDERS_URI + "/{id}/status", 404L)
                .contentType(MediaType.APPLICATION_JSON)
                .body(jsonMapper.writeValueAsString(request))
                .exchange();

        // Then
        response.expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.message").isEqualTo("Order not found with id 404")
                .jsonPath("$.path").isEqualTo(ORDERS_URI + "/404/status");
        then(orderService).should().updateStatus(404L, OrderStatus.CONFIRMED);
    }

    @Test
    void should_return_200_with_the_cancelled_order_when_it_is_cancelled() {
        // Given
        given(orderService.cancelOrder(5L)).willReturn(orderResponse(5L, "ORD-0005", OrderStatus.CANCELLED));

        // When
        var response = restTestClient.post().uri(ORDERS_URI + "/{id}/cancel", 5L).exchange();

        // Then
        response.expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.id").isEqualTo(5)
                .jsonPath("$.status").isEqualTo("CANCELLED");
        then(orderService).should().cancelOrder(5L);
    }

    @Test
    void should_return_409_when_the_order_is_already_cancelled() {
        // Given
        given(orderService.cancelOrder(5L))
                .willThrow(new DuplicateResourceException("Order ORD-0005 is already cancelled"));

        // When
        var response = restTestClient.post().uri(ORDERS_URI + "/{id}/cancel", 5L).exchange();

        // Then
        response.expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody()
                .jsonPath("$.status").isEqualTo(409)
                .jsonPath("$.error").isEqualTo("Conflict")
                .jsonPath("$.message").isEqualTo("Order ORD-0005 is already cancelled")
                .jsonPath("$.path").isEqualTo(ORDERS_URI + "/5/cancel");
        then(orderService).should().cancelOrder(5L);
    }

    @Test
    void should_return_404_when_cancelling_an_unknown_order() {
        // Given
        given(orderService.cancelOrder(404L)).willThrow(new ResourceNotFoundException("Order not found with id 404"));

        // When
        var response = restTestClient.post().uri(ORDERS_URI + "/{id}/cancel", 404L).exchange();

        // Then
        response.expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.message").isEqualTo("Order not found with id 404")
                .jsonPath("$.path").isEqualTo(ORDERS_URI + "/404/cancel");
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
