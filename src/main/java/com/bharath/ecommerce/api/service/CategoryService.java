package com.bharath.ecommerce.api.service;

import com.bharath.ecommerce.api.dto.CategoryResponse;
import com.bharath.ecommerce.api.dto.CreateCategoryRequest;
import com.bharath.ecommerce.api.entity.Category;
import com.bharath.ecommerce.api.exception.DuplicateResourceException;
import com.bharath.ecommerce.api.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;

    @Transactional
    public CategoryResponse create(CreateCategoryRequest request) {
        String name = request.getName().trim();
        String slug = request.getSlug().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("Category name already exists: " + name);
        }
        if (categoryRepository.existsBySlugIgnoreCase(slug)) {
            throw new DuplicateResourceException("Category slug already exists: " + slug);
        }

        Category category = Category.builder()
                .name(name)
                .slug(slug)
                .description(trimToNull(request.getDescription()))
                .build();
        return toResponse(categoryRepository.save(category));
    }

    private CategoryResponse toResponse(Category category) {
        return CategoryResponse.builder().id(category.getId()).name(category.getName())
                .slug(category.getSlug()).description(category.getDescription())
                .createdAt(category.getCreatedAt()).updatedAt(category.getUpdatedAt()).build();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
