package com.bharath.ecommerce.api.service;

import com.bharath.ecommerce.api.dto.CreateCategoryRequest;
import com.bharath.ecommerce.api.entity.Category;
import com.bharath.ecommerce.api.exception.DuplicateResourceException;
import com.bharath.ecommerce.api.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;

@SpringJUnitConfig(CategoryService.class)
class CategoryServiceTest {
    @MockitoBean
    private CategoryRepository categoryRepository;

    @Autowired
    private CategoryService service;

    @Test
    void should_normalize_name_and_description_when_creating_category() {
        // Given
        given(categoryRepository.save(any(Category.class))).willAnswer(invocation -> invocation.getArgument(0));
        var createCategoryRequest = CreateCategoryRequest.builder().name("  Home Office ").slug("home-office")
                .description("  Desks and chairs  ").build();

        // When
        service.create(createCategoryRequest);

        // Then
        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        then(categoryRepository).should().save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Home Office");
        assertThat(captor.getValue().getDescription()).isEqualTo("Desks and chairs");
    }

    @Test
    void should_throw_duplicate_resource_when_category_name_exists() {
        // Given
        given(categoryRepository.existsByNameIgnoreCase("Home Office")).willReturn(true);
        var createCategoryRequest = CreateCategoryRequest.builder()
                .name("  Home Office  ").slug("home-office").build();

        // When
        var thrown = catchThrowable(() -> service.create(createCategoryRequest));

        // Then
        assertThat(thrown).isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Category name already exists: Home Office");
        then(categoryRepository).should(never()).existsBySlugIgnoreCase(any());
        then(categoryRepository).should(never()).save(any());
    }

    @Test
    void should_throw_duplicate_resource_when_category_slug_exists() {
        // Given
        given(categoryRepository.existsBySlugIgnoreCase("home-office")).willReturn(true);
        var createCategoryRequest = CreateCategoryRequest.builder()
                .name("Home Office").slug("home-office").build();

        // When
        var thrown = catchThrowable(() -> service.create(createCategoryRequest));

        // Then
        assertThat(thrown).isInstanceOf(DuplicateResourceException.class);
        then(categoryRepository).should(never()).save(any());
    }
}
