package com.bharath.ecommerce.api.service;

import com.bharath.ecommerce.api.dto.CreateCategoryRequest;
import com.bharath.ecommerce.api.entity.Category;
import com.bharath.ecommerce.api.exception.DuplicateResourceException;
import com.bharath.ecommerce.api.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {
    @Mock
    private CategoryRepository categoryRepository;

    @Test
    void createsCategoryAfterNormalizingText() {
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CategoryService service = new CategoryService(categoryRepository);

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
        CategoryService service = new CategoryService(categoryRepository);

        assertThatThrownBy(() -> service.create(CreateCategoryRequest.builder()
                .name("  Home Office  ").slug("home-office").build()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Category name already exists: Home Office");
        verify(categoryRepository, never()).existsBySlugIgnoreCase(any());
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void rejectsDuplicateCategorySlug() {
        when(categoryRepository.existsBySlugIgnoreCase("home-office")).thenReturn(true);
        CategoryService service = new CategoryService(categoryRepository);

        assertThatThrownBy(() -> service.create(CreateCategoryRequest.builder()
                .name("Home Office").slug("home-office").build()))
                .isInstanceOf(DuplicateResourceException.class);
        verify(categoryRepository, never()).save(any());
    }
}
