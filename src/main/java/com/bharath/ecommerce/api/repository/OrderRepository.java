package com.bharath.ecommerce.api.repository;

import com.bharath.ecommerce.api.entity.Order;
import com.bharath.ecommerce.api.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNumber(String orderNumber);
    List<Order> findByCustomerId(Long customerId);
    List<Order> findByStatus(OrderStatus status);
}
