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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private CustomerRepository customerRepository;

    @Test
    void placeOrderRejectsUnknownCustomer() {
        when(customerRepository.findById(42L)).thenReturn(Optional.empty());
        OrderService service = service();

        assertThatThrownBy(() -> service.placeOrder(orderRequest(42L, 10L, 2)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Customer not found with id 42");
        verify(productRepository, never()).findAllByIdForUpdate(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void placeOrderRejectsInsufficientStock() {
        Customer customer = Customer.builder().id(42L).build();
        Product product = Product.builder().id(10L).active(true).stockQuantity(1)
                .price(new BigDecimal("20.00")).build();
        when(customerRepository.findById(42L)).thenReturn(Optional.of(customer));
        when(productRepository.findAllByIdForUpdate(any())).thenReturn(List.of(product));
        OrderService service = service();

        assertThatThrownBy(() -> service.placeOrder(orderRequest(42L, 10L, 2)))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessage("Insufficient stock for product 10: requested 2, available 1");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void cancelOrderRejectsDeliveredOrder() {
        Order order = Order.builder().id(5L).orderNumber("ORD-5").status(OrderStatus.DELIVERED).build();
        when(orderRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(order));
        OrderService service = service();

        assertThatThrownBy(() -> service.cancelOrder(5L))
                .isInstanceOf(InvalidOrderStatusTransitionException.class);
        verify(productRepository, never()).findAllByIdForUpdate(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void cancelOrderRejectsAlreadyCancelledOrder() {
        Order order = Order.builder().id(5L).orderNumber("ORD-5").status(OrderStatus.CANCELLED).build();
        when(orderRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(order));
        OrderService service = service();

        assertThatThrownBy(() -> service.cancelOrder(5L))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Order ORD-5 is already cancelled");
        verify(productRepository, never()).findAllByIdForUpdate(any());
        verify(orderRepository, never()).save(any());
    }

    private OrderService service() {
        return new OrderService(orderRepository, productRepository, customerRepository);
    }

    private CreateOrderRequest orderRequest(Long customerId, Long productId, int quantity) {
        return CreateOrderRequest.builder().customerId(customerId).shippingAddress("Main Street 1")
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .items(List.of(OrderItemRequest.builder().productId(productId).quantity(quantity).build()))
                .build();
    }
}
