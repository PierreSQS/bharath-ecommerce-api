package com.bharath.ecommerce.api.repository;

import com.bharath.ecommerce.api.entity.Payment;
import com.bharath.ecommerce.api.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderId(Long orderId);
    List<Payment> findByPaymentStatus(PaymentStatus paymentStatus);
}
