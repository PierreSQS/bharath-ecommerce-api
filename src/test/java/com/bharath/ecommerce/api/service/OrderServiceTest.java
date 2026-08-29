package com.bharath.ecommerce.api.service;

import com.bharath.ecommerce.api.dto.CreateOrderRequest;
import com.bharath.ecommerce.api.dto.OrderItemRequest;
import com.bharath.ecommerce.api.entity.Customer;
import com.bharath.ecommerce.api.entity.Order;
import com.bharath.ecommerce.api.entity.OrderStatus;
import com.bharath.ecommerce.api.entity.PaymentMethod;
import com.bharath.ecommerce.api.entity.Product;
import com.bharath.ecommerce.api.exception.DuplicateResourceException;
import com.bharath.ecommerce.api.exception.InsufficientStockException;
import com.bharath.ecommerce.api.exception.InvalidOrderStatusTransitionException;
import com.bharath.ecommerce.api.exception.ResourceNotFoundException;
import com.bharath.ecommerce.api.repository.CustomerRepository;
import com.bharath.ecommerce.api.repository.OrderRepository;
import com.bharath.ecommerce.api.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;

@SpringJUnitConfig(OrderService.class)
class OrderServiceTest {
    @MockitoBean
    private OrderRepository orderRepository;
    @MockitoBean
    private ProductRepository productRepository;
    @MockitoBean
    private CustomerRepository customerRepository;

    @Autowired
    private OrderService service;

    @Test
    void should_throw_resource_not_found_when_customer_missing() {
        // Given
        given(customerRepository.findById(42L)).willReturn(Optional.empty());
        var createOrderRequest = orderRequest(42L, 10L, 2);

        // When
        var thrown = catchThrowable(() -> service.placeOrder(createOrderRequest));

        // Then
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Customer not found with id 42");
        then(productRepository).should(never()).findAllByIdForUpdate(any());
        then(orderRepository).should(never()).save(any());
    }

    @Test
    void should_throw_insufficient_stock_when_quantity_exceeds_available() {
        // Given
        Customer customer = Customer.builder().id(42L).build();
        Product product = Product.builder().id(10L).active(true).stockQuantity(1)
                .price(new BigDecimal("20.00")).build();
        given(customerRepository.findById(42L)).willReturn(Optional.of(customer));
        given(productRepository.findAllByIdForUpdate(any())).willReturn(List.of(product));
        var createOrderRequest = orderRequest(42L, 10L, 2);

        // When
        var thrown = catchThrowable(() -> service.placeOrder(createOrderRequest));

        // Then
        assertThat(thrown).isInstanceOf(InsufficientStockException.class)
                .hasMessage("Insufficient stock for product 10: requested 2, available 1");
        then(orderRepository).should(never()).save(any());
    }

    @Test
    void should_throw_invalid_status_transition_when_cancelling_delivered_order() {
        // Given
        Order order = Order.builder().id(5L).orderNumber("ORD-5").status(OrderStatus.DELIVERED).build();
        given(orderRepository.findByIdForUpdate(5L)).willReturn(Optional.of(order));

        // When
        var thrown = catchThrowable(() -> service.cancelOrder(5L));

        // Then
        assertThat(thrown).isInstanceOf(InvalidOrderStatusTransitionException.class);
        then(productRepository).should(never()).findAllByIdForUpdate(any());
        then(orderRepository).should(never()).save(any());
    }

    @Test
    void should_throw_duplicate_resource_when_cancelling_cancelled_order() {
        // Given
        Order order = Order.builder().id(5L).orderNumber("ORD-5").status(OrderStatus.CANCELLED).build();
        given(orderRepository.findByIdForUpdate(5L)).willReturn(Optional.of(order));

        // When
        var thrown = catchThrowable(() -> service.cancelOrder(5L));

        // Then
        assertThat(thrown).isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Order ORD-5 is already cancelled");
        then(productRepository).should(never()).findAllByIdForUpdate(any());
        then(orderRepository).should(never()).save(any());
    }

    private CreateOrderRequest orderRequest(Long customerId, Long productId, int quantity) {
        return CreateOrderRequest.builder().customerId(customerId).shippingAddress("Main Street 1")
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .items(List.of(OrderItemRequest.builder().productId(productId).quantity(quantity).build()))
                .build();
    }
}
