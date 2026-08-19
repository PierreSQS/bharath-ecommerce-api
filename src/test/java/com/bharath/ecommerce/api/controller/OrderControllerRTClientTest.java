package com.bharath.ecommerce.api.controller;

import com.bharath.ecommerce.api.dto.CreateOrderRequest;
import com.bharath.ecommerce.api.dto.OrderItemRequest;
import com.bharath.ecommerce.api.dto.OrderResponse;
import com.bharath.ecommerce.api.dto.UpdateOrderStatusRequest;
import com.bharath.ecommerce.api.entity.OrderStatus;
import com.bharath.ecommerce.api.entity.PaymentMethod;
import com.bharath.ecommerce.api.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
@WebMvcTest(OrderController.class)
@AutoConfigureRestTestClient
class OrderControllerRTClientTest {
    @Autowired private RestTestClient restTestClient;
    @MockitoBean private OrderService orderService;

    private OrderResponse order(long id, OrderStatus status) {
        return OrderResponse.builder().id(id).orderNumber("ORD-1001").customerId(3L).customerName("Ada Lovelace")
                .status(status).totalAmount(new BigDecimal("79.99")).shippingAddress("Berlin").items(List.of()).build();
    }

    @Test
    void should_returnOrders_when_ordersExist() throws Exception {
        // Arrange
        when(orderService.getAllOrders()).thenReturn(List.of(order(21L, OrderStatus.PENDING)));

        // Act
        var result = restTestClient.get().uri("/api/v1/orders").exchange();

        // Assert
        result.expectStatus().isOk().expectBody()
                .jsonPath("$[0].id").isEqualTo(21)
                .jsonPath("$[0].orderNumber").isEqualTo("ORD-1001");
    }

    @Test
    void should_placeOrder_when_requestIsValid() throws Exception {
        // Arrange
        var request = CreateOrderRequest.builder().customerId(3L).shippingAddress("Berlin")
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .items(List.of(OrderItemRequest.builder().productId(11L).quantity(1).build())).build();
        when(orderService.placeOrder(any(CreateOrderRequest.class))).thenReturn(order(21L, OrderStatus.PENDING));

        // Act
        var result = restTestClient.post().uri("/api/v1/orders").body(request).exchange();

        // Assert
        result.expectStatus().isCreated()
                .expectHeader().valueEquals("Location", "http://localhost/api/v1/orders/21")
                .expectBody().jsonPath("$.id").isEqualTo(21)
                .jsonPath("$.status").isEqualTo("PENDING");
    }

    @Test
    void should_rejectRequest_when_itemsAreEmpty() throws Exception {
        // Arrange
        var request = CreateOrderRequest.builder().customerId(3L).shippingAddress("Berlin")
                .paymentMethod(PaymentMethod.CREDIT_CARD).items(List.of()).build();

        // Act
        var result = restTestClient.post().uri("/api/v1/orders").body(request).exchange();

        // Assert
        result.expectStatus().isBadRequest();
    }

    @Test
    void should_returnOrder_when_idExists() throws Exception {
        // Arrange
        when(orderService.getOrder(21L)).thenReturn(order(21L, OrderStatus.PENDING));

        // Act
        var result = restTestClient.get().uri("/api/v1/orders/21").exchange();

        // Assert
        result.expectStatus().isOk().expectBody().jsonPath("$.id").isEqualTo(21);
    }

    @Test
    void should_returnOrder_when_orderNumberExists() throws Exception {
        // Arrange
        when(orderService.getOrderByNumber("ORD-1001")).thenReturn(order(21L, OrderStatus.PENDING));

        // Act
        var result = restTestClient.get().uri("/api/v1/orders/number/ORD-1001").exchange();

        // Assert
        result.expectStatus().isOk().expectBody().jsonPath("$.orderNumber").isEqualTo("ORD-1001");
    }

    @Test
    void should_updateStatus_when_requestIsValid() throws Exception {
        // Arrange
        var request = UpdateOrderStatusRequest.builder().status(OrderStatus.CONFIRMED).build();
        when(orderService.updateStatus(21L, OrderStatus.CONFIRMED)).thenReturn(order(21L, OrderStatus.CONFIRMED));

        // Act
        var result = restTestClient.patch().uri("/api/v1/orders/21/status").body(request).exchange();

        // Assert
        result.expectStatus().isOk().expectBody().jsonPath("$.status").isEqualTo("CONFIRMED");
    }

    @Test
    void should_rejectRequest_when_statusIsMissing() throws Exception {
        // Arrange
        var request = new UpdateOrderStatusRequest();

        // Act
        var result = restTestClient.patch().uri("/api/v1/orders/21/status").body(request).exchange();

        // Assert
        result.expectStatus().isBadRequest();
    }

    @Test
    void should_cancelOrder_when_idExists() throws Exception {
        // Arrange
        when(orderService.cancelOrder(21L)).thenReturn(order(21L, OrderStatus.CANCELLED));

        // Act
        var result = restTestClient.post().uri("/api/v1/orders/21/cancel").exchange();

        // Assert
        result.expectStatus().isOk().expectBody().jsonPath("$.status").isEqualTo("CANCELLED");
    }
}
