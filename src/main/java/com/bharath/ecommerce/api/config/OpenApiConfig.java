package com.bharath.ecommerce.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    /**
     * No {@code servers} are declared on purpose: springdoc then derives the server URL from the
     * incoming request, so "Try it out" keeps working locally, in Docker and behind a proxy.
     */
    @Bean
    OpenAPI ecommerceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("E-Commerce API")
                .version("v1")
                .description("""
                        REST API for the e-commerce domain: categories, products, customers, orders and payments.

                        **Resources**
                        - `/api/v1/categories`
                        - `/api/v1/products`
                        - `/api/v1/customers`
                        - `/api/v1/orders`

                        **Error handling**

                        Every failure is returned as the same `ErrorResponse` JSON payload:

                        | Status | Meaning |
                        |--------|---------|
                        | `400 Bad Request` | Validation failure; the response lists all offending fields. |
                        | `404 Not Found` | The requested resource does not exist. |
                        | `409 Conflict` | The resource already exists. |
                        | `422 Unprocessable Content` | A business rule was violated (e.g. insufficient stock, illegal order status transition). |
                        """));
    }
}
