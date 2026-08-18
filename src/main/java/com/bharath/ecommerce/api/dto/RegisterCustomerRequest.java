package com.bharath.ecommerce.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RegisterCustomerRequest {
    @NotBlank @Size(max = 100)
    private String firstName;

    @NotBlank @Size(max = 100)
    private String lastName;

    @NotBlank @Email @Size(max = 255)
    private String email;

    @Size(max = 20)
    @Pattern(regexp = "^$|^[+0-9][0-9 .()/-]{5,19}$", message = "must be a valid phone number")
    private String phone;

    @Size(max = 2000)
    private String address;
}
