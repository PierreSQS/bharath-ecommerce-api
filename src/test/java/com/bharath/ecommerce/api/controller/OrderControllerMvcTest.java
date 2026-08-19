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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerMvcTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
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
        var result = mockMvc.perform(get("/api/v1/orders"));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(21))
                .andExpect(jsonPath("$[0].orderNumber").value("ORD-1001"));
    }

    @Test
    void should_placeOrder_when_requestIsValid() throws Exception {
        // Arrange
        var request = CreateOrderRequest.builder().customerId(3L).shippingAddress("Berlin")
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .items(List.of(OrderItemRequest.builder().productId(11L).quantity(1).build())).build();
        when(orderService.placeOrder(any(CreateOrderRequest.class))).thenReturn(order(21L, OrderStatus.PENDING));

        // Act
        var result = mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // Assert
        result.andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/orders/21"))
                .andExpect(jsonPath("$.id").value(21))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void should_rejectRequest_when_itemsAreEmpty() throws Exception {
        // Arrange
        var request = CreateOrderRequest.builder().customerId(3L).shippingAddress("Berlin")
                .paymentMethod(PaymentMethod.CREDIT_CARD).items(List.of()).build();

        // Act
        var result = mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // Assert
        result.andExpect(status().isBadRequest());
    }

    @Test
    void should_returnOrder_when_idExists() throws Exception {
        // Arrange
        when(orderService.getOrder(21L)).thenReturn(order(21L, OrderStatus.PENDING));

        // Act
        var result = mockMvc.perform(get("/api/v1/orders/21"));

        // Assert
        result.andExpect(status().isOk()).andExpect(jsonPath("$.id").value(21));
    }

    @Test
    void should_returnOrder_when_orderNumberExists() throws Exception {
        // Arrange
        when(orderService.getOrderByNumber("ORD-1001")).thenReturn(order(21L, OrderStatus.PENDING));

        // Act
        var result = mockMvc.perform(get("/api/v1/orders/number/ORD-1001"));

        // Assert
        result.andExpect(status().isOk()).andExpect(jsonPath("$.orderNumber").value("ORD-1001"));
    }

    @Test
    void should_updateStatus_when_requestIsValid() throws Exception {
        // Arrange
        var request = UpdateOrderStatusRequest.builder().status(OrderStatus.CONFIRMED).build();
        when(orderService.updateStatus(21L, OrderStatus.CONFIRMED)).thenReturn(order(21L, OrderStatus.CONFIRMED));

        // Act
        var result = mockMvc.perform(patch("/api/v1/orders/21/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // Assert
        result.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void should_rejectRequest_when_statusIsMissing() throws Exception {
        // Arrange
        var request = new UpdateOrderStatusRequest();

        // Act
        var result = mockMvc.perform(patch("/api/v1/orders/21/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // Assert
        result.andExpect(status().isBadRequest());
    }

    @Test
    void should_cancelOrder_when_idExists() throws Exception {
        // Arrange
        when(orderService.cancelOrder(21L)).thenReturn(order(21L, OrderStatus.CANCELLED));

        // Act
        var result = mockMvc.perform(post("/api/v1/orders/21/cancel"));

        // Assert
        result.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
