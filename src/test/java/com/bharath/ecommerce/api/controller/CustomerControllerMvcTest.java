package com.bharath.ecommerce.api.controller;

import com.bharath.ecommerce.api.dto.CustomerResponse;
import com.bharath.ecommerce.api.dto.RegisterCustomerRequest;
import com.bharath.ecommerce.api.exception.DuplicateResourceException;
import com.bharath.ecommerce.api.service.CustomerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerController.class)
class CustomerControllerMvcTest {

    private static final String CUSTOMERS_URI = "/api/v1/customers";

    @MockitoBean
    private CustomerService customerService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void should_return_201_with_location_and_body_when_customer_is_registered() throws Exception {
        // Given
        var request = RegisterCustomerRequest.builder().firstName("Ada").lastName("Lovelace")
                .email("ada@example.com").phone("+33 1 23 45 67").address("12 Analytical Street").build();
        var response = CustomerResponse.builder().id(42L).firstName("Ada").lastName("Lovelace")
                .email("ada@example.com").phone("+33 1 23 45 67").address("12 Analytical Street")
                .createdAt(LocalDateTime.of(2026, 3, 4, 5, 6)).build();
        given(customerService.register(any(RegisterCustomerRequest.class))).willReturn(response);

        // When
        var result = mockMvc.perform(post(CUSTOMERS_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)));

        // Then
        result.andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost" + CUSTOMERS_URI + "/42"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.firstName").value("Ada"))
                .andExpect(jsonPath("$.lastName").value("Lovelace"))
                .andExpect(jsonPath("$.email").value("ada@example.com"));
        then(customerService).should().register(any(RegisterCustomerRequest.class));
    }

    @Test
    void should_return_400_with_all_field_errors_when_request_is_invalid() throws Exception {
        // Given
        var request = RegisterCustomerRequest.builder().firstName("  ").lastName("")
                .email("not-an-email").phone("abc").build();

        // When
        var result = mockMvc.perform(post(CUSTOMERS_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)));

        // Then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.path").value(CUSTOMERS_URI))
                .andExpect(jsonPath("$.validationErrors.firstName").exists())
                .andExpect(jsonPath("$.validationErrors.lastName").exists())
                .andExpect(jsonPath("$.validationErrors.email").exists())
                .andExpect(jsonPath("$.validationErrors.phone").value("must be a valid phone number"));
        then(customerService).should(never()).register(any());
    }

    @Test
    void should_return_409_when_customer_email_is_already_registered() throws Exception {
        // Given
        var request = RegisterCustomerRequest.builder().firstName("Ada").lastName("Lovelace")
                .email("ada@example.com").build();
        given(customerService.register(any(RegisterCustomerRequest.class)))
                .willThrow(new DuplicateResourceException("Customer email already registered: ada@example.com"));

        // When
        var result = mockMvc.perform(post(CUSTOMERS_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)));

        // Then
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Customer email already registered: ada@example.com"))
                .andExpect(jsonPath("$.path").value(CUSTOMERS_URI))
                .andExpect(jsonPath("$.validationErrors").doesNotExist());
        then(customerService).should().register(any(RegisterCustomerRequest.class));
    }
}
