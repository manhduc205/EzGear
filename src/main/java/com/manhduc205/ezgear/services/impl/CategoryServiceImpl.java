package com.manhduc205.ezgear.services.impl;

import com.manhduc205.ezgear.dtos.CategoryDTO;
import com.manhduc205.ezgear.models.Category;
import com.manhduc205.ezgear.repositories.CategoryRepository;
import com.manhduc205.ezgear.services.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public Category createCategory(CategoryDTO category) {
        Category newCategory = Category.builder()
                .name(category.getName())
                .build();

        return categoryRepository.save(newCategory);
    }

    @Override
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Override
    public Category getCategoryById(Long categoryId) {
        return categoryRepository.findById(categoryId).
                orElseThrow(() -> new RuntimeException());
    }

    @Override
    public Category updateCategory(Long categoryId, CategoryDTO category) {
        Category oldCategory = getCategoryById(categoryId);
        oldCategory.setName(category.getName());
        return categoryRepository.save(oldCategory);
    }

    @Override
    public void deleteCategory(Long categoryId) {
        categoryRepository.deleteById(categoryId);
    }
}
