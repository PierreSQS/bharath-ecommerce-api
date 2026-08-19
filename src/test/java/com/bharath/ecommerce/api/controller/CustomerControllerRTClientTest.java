package com.bharath.ecommerce.api.controller;

import com.bharath.ecommerce.api.dto.CustomerResponse;
import com.bharath.ecommerce.api.dto.RegisterCustomerRequest;
import com.bharath.ecommerce.api.service.CustomerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
@WebMvcTest(CustomerController.class)
@AutoConfigureRestTestClient
class CustomerControllerRTClientTest {
    @Autowired private RestTestClient restTestClient;
    @MockitoBean private CustomerService customerService;

    @Test
    void should_registerCustomer_when_requestIsValid() {
        // Arrange
        var request = RegisterCustomerRequest.builder().firstName("Ada").lastName("Lovelace")
                .email("ada@example.com").phone("+49 12345678").build();
        var response = CustomerResponse.builder().id(3L).firstName("Ada").lastName("Lovelace")
                .email("ada@example.com").phone("+49 12345678").build();
        when(customerService.register(any(RegisterCustomerRequest.class))).thenReturn(response);

        // Act
        var result = restTestClient.post().uri("/api/v1/customers").body(request).exchange();

        // Assert
        result.expectStatus().isCreated()
                .expectHeader().valueEquals("Location", "http://localhost/api/v1/customers/3")
                .expectBody().jsonPath("$.id").isEqualTo(3)
                .jsonPath("$.email").isEqualTo("ada@example.com");
    }

    @Test
    void should_rejectRequest_when_emailIsInvalid() {
        // Arrange
        var request = RegisterCustomerRequest.builder().firstName("Ada").lastName("Lovelace")
                .email("not-an-email").build();

        // Act
        var result = restTestClient.post().uri("/api/v1/customers").body(request).exchange();

        // Assert
        result.expectStatus().isBadRequest();
    }
}
