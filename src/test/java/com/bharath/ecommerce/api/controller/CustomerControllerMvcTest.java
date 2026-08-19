package com.bharath.ecommerce.api.controller;

import com.bharath.ecommerce.api.dto.CustomerResponse;
import com.bharath.ecommerce.api.dto.RegisterCustomerRequest;
import com.bharath.ecommerce.api.service.CustomerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerController.class)
class CustomerControllerMvcTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper jsonMapper;
    @MockitoBean private CustomerService customerService;

    @Test
    void should_getAllCustomers() throws Exception {
        when(customerService.getAll()).thenReturn(List.of(CustomerResponse.builder().id(3L)
                .firstName("Ada").lastName("Lovelace").email("ada@example.com").build()));

        mockMvc.perform(get("/api/v1/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(3))
                .andExpect(jsonPath("$[0].email").value("ada@example.com"));
    }

    @Test
    void should_registerCustomer_when_requestIsValid() throws Exception {
        // Arrange
        var request = RegisterCustomerRequest.builder().firstName("Ada").lastName("Lovelace")
                .email("ada@example.com").phone("+49 12345678").build();
        var response = CustomerResponse.builder().id(3L).firstName("Ada").lastName("Lovelace")
                .email("ada@example.com").phone("+49 12345678").build();
        when(customerService.register(any(RegisterCustomerRequest.class))).thenReturn(response);

        // Act
        var result = mockMvc.perform(post("/api/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)));

        // Assert
        result.andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/customers/3"))
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.email").value("ada@example.com"));
    }

    @Test
    void should_rejectRequest_when_emailIsInvalid() throws Exception {
        // Arrange
        var request = RegisterCustomerRequest.builder().firstName("Ada").lastName("Lovelace")
                .email("not-an-email").build();

        // Act
        var result = mockMvc.perform(post("/api/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)));

        // Assert
        result.andExpect(status().isBadRequest());
    }
}
