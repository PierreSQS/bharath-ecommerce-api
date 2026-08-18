package com.bharath.ecommerce.api.exception;

import com.bharath.ecommerce.api.entity.OrderStatus;

public class InvalidOrderStatusTransitionException extends RuntimeException {
    public InvalidOrderStatusTransitionException(OrderStatus current, OrderStatus requested) {
        super("Order status cannot transition from " + current + " to " + requested);
    }
}
