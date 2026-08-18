package com.bharath.ecommerce.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreateCategoryRequest {
    @NotBlank @Size(max = 100)
    private String name;

    @NotBlank @Size(max = 100)
    @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*",
            message = "must contain lowercase letters, numbers, and single hyphens only")
    private String slug;

    @Size(max = 2000)
    private String description;
}
