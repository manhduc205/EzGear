package com.manhduc205.ezgear.services;

import com.manhduc205.ezgear.dto.CategoryDTO;
import com.manhduc205.ezgear.models.Category;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface CategoryService {
    Category createCategory(CategoryDTO category);
    List<Category> getAllCategories();
    Category updateCategory(Long categoryId, CategoryDTO category);
    Category getCategoryById(Long categoryId);
    void deleteCategory(Long categoryId);
}
