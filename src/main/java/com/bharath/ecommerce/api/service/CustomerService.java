package com.bharath.ecommerce.api.service;

import com.bharath.ecommerce.api.dto.CustomerResponse;
import com.bharath.ecommerce.api.dto.RegisterCustomerRequest;
import com.bharath.ecommerce.api.entity.Customer;
import com.bharath.ecommerce.api.exception.DuplicateResourceException;
import com.bharath.ecommerce.api.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CustomerService {
    private final CustomerRepository customerRepository;

    @Transactional
    public CustomerResponse register(RegisterCustomerRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (customerRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("Customer email already registered: " + email);
        }

        Customer customer = Customer.builder()
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .email(email)
                .phone(trimToNull(request.getPhone()))
                .address(trimToNull(request.getAddress()))
                .build();
        return toResponse(customerRepository.save(customer));
    }

    private CustomerResponse toResponse(Customer customer) {
        return CustomerResponse.builder().id(customer.getId()).firstName(customer.getFirstName())
                .lastName(customer.getLastName()).email(customer.getEmail()).phone(customer.getPhone())
                .address(customer.getAddress()).createdAt(customer.getCreatedAt()).build();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
