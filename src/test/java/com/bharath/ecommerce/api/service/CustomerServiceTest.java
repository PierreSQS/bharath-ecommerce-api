package com.bharath.ecommerce.api.service;

import com.bharath.ecommerce.api.dto.RegisterCustomerRequest;
import com.bharath.ecommerce.api.entity.Customer;
import com.bharath.ecommerce.api.exception.DuplicateResourceException;
import com.bharath.ecommerce.api.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {
    @Mock
    private CustomerRepository customerRepository;

    @Test
    void registersCustomerWithCanonicalEmail() {
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CustomerService service = new CustomerService(customerRepository);

        var response = service.register(RegisterCustomerRequest.builder().firstName(" Ana ").lastName(" Smith ")
                .email(" Ana.Smith@Example.COM ").phone(" +49 123456 ").build());

        assertThat(response.getFirstName()).isEqualTo("Ana");
        assertThat(response.getEmail()).isEqualTo("ana.smith@example.com");
        assertThat(response.getPhone()).isEqualTo("+49 123456");
    }

    @Test
    void rejectsDuplicateEmail() {
        when(customerRepository.existsByEmailIgnoreCase("ana@example.com")).thenReturn(true);
        CustomerService service = new CustomerService(customerRepository);

        assertThatThrownBy(() -> service.register(RegisterCustomerRequest.builder()
                .firstName("Ana").lastName("Smith").email(" ANA@example.com ").build()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Customer email already registered: ana@example.com");
        verify(customerRepository, never()).save(any());
    }
}
