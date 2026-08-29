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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(CategoryService.class)
class CategoryServiceTest {
    @MockitoBean
    private CategoryRepository categoryRepository;

    @Autowired
    private CategoryService service;

    @Test
    void createsCategoryAfterNormalizingText() {
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));
        service.create(CreateCategoryRequest.builder().name("  Home Office ").slug("home-office")
                .description("  Desks and chairs  ").build());

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Home Office");
        assertThat(captor.getValue().getDescription()).isEqualTo("Desks and chairs");
    }

    @Test
    void rejectsDuplicateCategoryName() {
        when(categoryRepository.existsByNameIgnoreCase("Home Office")).thenReturn(true);
        var createCategoryRequest = CreateCategoryRequest.builder()
                .name("  Home Office  ").slug("home-office").build();

        assertThatThrownBy(() -> service.create(createCategoryRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Category name already exists: Home Office");

        verify(categoryRepository, never()).existsBySlugIgnoreCase(any());
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void rejectsDuplicateCategorySlug() {
        when(categoryRepository.existsBySlugIgnoreCase("home-office")).thenReturn(true);
        var createCategoryRequest = CreateCategoryRequest.builder()
                .name("Home Office").slug("home-office").build();

        assertThatThrownBy(() -> service.create(createCategoryRequest))
                .isInstanceOf(DuplicateResourceException.class);

        verify(categoryRepository, never()).save(any());
    }
}
