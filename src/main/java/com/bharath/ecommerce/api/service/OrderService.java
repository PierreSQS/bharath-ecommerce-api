package com.bharath.ecommerce.api.service;

import com.bharath.ecommerce.api.dto.*;
import com.bharath.ecommerce.api.entity.*;
import com.bharath.ecommerce.api.exception.*;
import com.bharath.ecommerce.api.repository.CustomerRepository;
import com.bharath.ecommerce.api.repository.OrderRepository;
import com.bharath.ecommerce.api.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderService {
    private static final int LOW_STOCK_THRESHOLD = 10;

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
            OrderStatus.PENDING, Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
            OrderStatus.CONFIRMED, Set.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED),
            OrderStatus.PROCESSING, Set.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED),
            OrderStatus.SHIPPED, Set.of(OrderStatus.DELIVERED)
    );

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;

    /** Creates the complete order aggregate so stock, items, and payment commit or roll back together. */
    @Transactional
    public OrderResponse placeOrder(CreateOrderRequest request) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id " + request.getCustomerId()));

        Map<Long, Integer> requestedQuantities = aggregateQuantities(request.getItems());
        // Locking in identifier order makes concurrent orders serialize on inventory and reduces deadlock risk.
        List<Product> lockedProducts = productRepository.findAllByIdForUpdate(requestedQuantities.keySet());
        Map<Long, Product> products = lockedProducts.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        validateProducts(requestedQuantities, products);

        Order order = Order.builder()
                .customer(customer)
                .status(OrderStatus.PENDING)
                .shippingAddress(request.getShippingAddress())
                .notes(request.getNotes())
                .build();

        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> entry : requestedQuantities.entrySet()) {
            Product product = products.get(entry.getKey());
            int quantity = entry.getValue();
            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(quantity));
            product.setStockQuantity(product.getStockQuantity() - quantity);
            order.addItem(OrderItem.builder().product(product).quantity(quantity)
                    .unitPrice(product.getPrice()).subtotal(subtotal).build());
            total = total.add(subtotal);
        }
        warnOnLowStock(products.values());
        order.setTotalAmount(total);
        order.setPayment(Payment.builder().paymentMethod(request.getPaymentMethod())
                .paymentStatus(PaymentStatus.PENDING).amount(total).build());
        return toResponse(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long id) {
        return toResponse(orderRepository.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id " + id)));
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderByNumber(String orderNumber) {
        return toResponse(orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with number " + orderNumber)));
    }

    /** Returns complete order DTOs while the read-only transaction owns all aggregate loading. */
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    /** Applies only explicit workflow edges; skipped, repeated, and backward changes are conflicts. */
    @Transactional
    public OrderResponse updateStatus(Long id, OrderStatus requestedStatus) {
        Order order = lockedOrder(id);
        validateTransition(order.getStatus(), requestedStatus);
        if (requestedStatus == OrderStatus.CANCELLED) {
            cancelAggregate(order);
        } else {
            order.setStatus(requestedStatus);
        }
        return toResponse(orderRepository.save(order));
    }

    /** Cancels under an order lock, ensuring concurrent retries cannot restore inventory twice. */
    @Transactional
    public OrderResponse cancelOrder(Long id) {
        Order order = lockedOrder(id);
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new DuplicateResourceException("Order " + order.getOrderNumber() + " is already cancelled");
        }
        validateTransition(order.getStatus(), OrderStatus.CANCELLED);
        cancelAggregate(order);
        return toResponse(orderRepository.save(order));
    }

    private Order lockedOrder(Long id) {
        return orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id " + id));
    }

    private Map<Long, Integer> aggregateQuantities(List<OrderItemRequest> items) {
        Map<Long, Integer> quantities = new TreeMap<>();
        for (OrderItemRequest item : items) {
            try {
                quantities.merge(item.getProductId(), item.getQuantity(), Math::addExact);
            } catch (ArithmeticException _) {
                throw new BusinessRuleException("Requested quantity is too large for product " + item.getProductId());
            }
        }
        return quantities;
    }

    private void validateProducts(Map<Long, Integer> quantities, Map<Long, Product> products) {
        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            Product product = products.get(entry.getKey());
            if (product == null) {
                throw new ResourceNotFoundException("Product not found with id " + entry.getKey());
            }
            if (!Boolean.TRUE.equals(product.getActive())) {
                throw new BusinessRuleException("Product " + product.getId() + " is not available for ordering");
            }
            if (product.getStockQuantity() < entry.getValue()) {
                throw new InsufficientStockException("Insufficient stock for product " + product.getId()
                        + ": requested " + entry.getValue() + ", available " + product.getStockQuantity());
            }
        }
    }

    private void warnOnLowStock(Collection<Product> products) {
        for (Product product : products) {
            if (product.getStockQuantity() < LOW_STOCK_THRESHOLD) {
                log.warn("Low stock alert: {} has {} units left", product.getName(), product.getStockQuantity());
            }
        }
    }

    private void validateTransition(OrderStatus current, OrderStatus requested) {
        if (!ALLOWED_TRANSITIONS.getOrDefault(current, Set.of()).contains(requested)) {
            throw new InvalidOrderStatusTransitionException(current, requested);
        }
    }

    private void cancelAggregate(Order order) {
        // Product locks coordinate restoration with new orders; the order lock prevents duplicate restoration.
        productRepository.findAllByIdForUpdate(order.getItems().stream()
                .map(item -> item.getProduct().getId()).sorted().toList());
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setStockQuantity(Math.addExact(product.getStockQuantity(), item.getQuantity()));
        }
        order.setStatus(OrderStatus.CANCELLED);
        Payment payment = order.getPayment();
        if (payment != null && payment.getPaymentStatus() == PaymentStatus.COMPLETED) {
            // No external provider exists; refunding records only the local payment outcome.
            payment.setPaymentStatus(PaymentStatus.REFUNDED);
        }
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream().map(item -> OrderItemResponse.builder()
                .id(item.getId()).productId(item.getProduct().getId()).productName(item.getProduct().getName())
                .sku(item.getProduct().getSku()).quantity(item.getQuantity()).unitPrice(item.getUnitPrice())
                .subtotal(item.getSubtotal()).build()).toList();
        Payment payment = order.getPayment();
        PaymentResponse paymentResponse = payment == null ? null : PaymentResponse.builder()
                .id(payment.getId()).paymentMethod(payment.getPaymentMethod()).paymentStatus(payment.getPaymentStatus())
                .amount(payment.getAmount()).createdAt(payment.getCreatedAt()).build();
        Customer customer = order.getCustomer();
        return OrderResponse.builder().id(order.getId()).orderNumber(order.getOrderNumber())
                .customerId(customer.getId()).customerName(customer.getFirstName() + " " + customer.getLastName())
                .status(order.getStatus()).totalAmount(order.getTotalAmount())
                .shippingAddress(order.getShippingAddress()).notes(order.getNotes())
                .createdAt(order.getCreatedAt()).updatedAt(order.getUpdatedAt())
                .items(items).payment(paymentResponse).build();
    }
}
