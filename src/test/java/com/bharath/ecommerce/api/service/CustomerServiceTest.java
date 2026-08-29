package com.bharath.ecommerce.api.service;

import com.bharath.ecommerce.api.dto.RegisterCustomerRequest;
import com.bharath.ecommerce.api.entity.Customer;
import com.bharath.ecommerce.api.exception.DuplicateResourceException;
import com.bharath.ecommerce.api.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;

@SpringJUnitConfig(CustomerService.class)
class CustomerServiceTest {
    @MockitoBean
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerService service;

    @Test
    void should_canonicalize_fields_when_registering_customer() {
        // Given
        given(customerRepository.save(any(Customer.class))).willAnswer(invocation -> invocation.getArgument(0));
        var registerCustomerRequest = RegisterCustomerRequest.builder().firstName(" Ana ").lastName(" Smith ")
                .email(" Ana.Smith@Example.COM ").phone(" +49 123456 ").build();

        // When
        var response = service.register(registerCustomerRequest);

        // Then
        assertThat(response.getFirstName()).isEqualTo("Ana");
        assertThat(response.getEmail()).isEqualTo("ana.smith@example.com");
        assertThat(response.getPhone()).isEqualTo("+49 123456");
    }

    @Test
    void should_throw_duplicate_resource_when_email_already_registered() {
        // Given
        given(customerRepository.existsByEmailIgnoreCase("ana@example.com")).willReturn(true);
        var registerCustomerRequest = RegisterCustomerRequest.builder()
                .firstName("Ana").lastName("Smith").email(" ANA@example.com ").build();

        // When
        var thrown = catchThrowable(() -> service.register(registerCustomerRequest));

        // Then
        assertThat(thrown).isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Customer email already registered: ana@example.com");
        then(customerRepository).should(never()).save(any());
    }
}
